package club.freecity.cms.service.impl;

import club.freecity.cms.common.TenantContext;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.dto.MediaAssetDto;
import club.freecity.cms.dto.StorageStatsDto;
import club.freecity.cms.entity.MediaAsset;
import club.freecity.cms.entity.Tenant;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.MediaAssetRepository;
import club.freecity.cms.repository.TenantRepository;
import club.freecity.cms.service.FileService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.upload.max-size:10MB}")
    private DataSize maxFileSize;

    private final MediaAssetRepository mediaAssetRepository;
    private final TenantRepository tenantRepository;

    // 允许上传的文件后缀白名单
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".jfif", ".png", ".gif", ".webp", ".svg", ".ico",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".md", ".zip", ".rar", ".7z"
    );

    // 允许上传的 MIME 类型白名单
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/jfif", "image/png", "image/gif", "image/webp", "image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon",
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/markdown", "application/zip", "application/x-rar-compressed", "application/x-7z-compressed"
    );

    // 文件头 Magic Number 映射表
    private static final Map<String, String> MAGIC_NUMBERS = new HashMap<>();
    static {
        MAGIC_NUMBERS.put("FFD8FF", ".jpg");
        MAGIC_NUMBERS.put("89504E47", ".png");
        MAGIC_NUMBERS.put("47494638", ".gif");
        MAGIC_NUMBERS.put("52494646", ".webp"); // WebP 实际上还有额外的校验，这里简化处理
        MAGIC_NUMBERS.put("25504446", ".pdf");
        MAGIC_NUMBERS.put("504B0304", ".zip"); // .docx, .xlsx, .pptx 也是这个头
        MAGIC_NUMBERS.put("D0CF11E0", ".doc"); // 旧版 Office 格式
        MAGIC_NUMBERS.put("52617221", ".rar");
        MAGIC_NUMBERS.put("377ABCAF", ".7z");
    }

    @Override
    @Transactional(readOnly = true)
    public StorageStatsDto getStorageStats() {
        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            return new StorageStatsDto(0L, 0L, 0.0);
        }

        // 1. 获取已用空间 (Byte)
        // 注意：由于启用了租户过滤器，这里只会查到当前租户的记录
        Long usedSize = mediaAssetRepository.findAll().stream()
                .mapToLong(MediaAsset::getSize)
                .sum();

        // 2. 获取总容量限制 (MB -> Byte)
        Long totalSizeInBytes = 100 * 1024 * 1024L; // 默认 100MB
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isPresent() && tenantOpt.get().getPackageInfo() != null) {
            totalSizeInBytes = tenantOpt.get().getPackageInfo().getMaxStorage() * 1024 * 1024L;
        }

        // 3. 计算百分比
        Double percentage = totalSizeInBytes > 0 ? (usedSize.doubleValue() / totalSizeInBytes.doubleValue()) * 100 : 0.0;
        
        return StorageStatsDto.builder()
                .usedSize(usedSize)
                .totalSize(totalSizeInBytes)
                .percentage(Math.min(100.0, percentage))
                .build();
    }

    @Override
    @Transactional
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 0. 校验文件大小
        if (file.getSize() > maxFileSize.toBytes()) {
            throw new BusinessException("文件大小超过限制 (最大 " + maxFileSize.toMegabytes() + "MB)");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        
        // 1. 校验后缀
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件格式: " + extension);
        }

        // 2. 校验 MIME 类型
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("不支持的文件类型: " + contentType);
        }

        // 3. 深度安全校验：校验文件头 Magic Number
        validateMagicNumber(file, extension);

        String tenantCode = TenantContext.getCurrentTenantCode();
        if (tenantCode == null) {
            tenantCode = "default";
        }

        // 租户目录隔离: uploads/{tenantCode}/yyyyMM/uuid.ext
        String datePath = java.time.format.DateTimeFormatter.ofPattern("yyyyMM").format(java.time.LocalDate.now());
        String fileName = UUID.randomUUID().toString() + extension;

        // 确保路径是绝对路径，避免被 Tomcat 解释为相对临时目录的路径
        Path rootPath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path tenantDir = rootPath.resolve(Paths.get(tenantCode, datePath));
        
        try {
            if (!Files.exists(tenantDir)) {
                Files.createDirectories(tenantDir);
            }
            Path targetFile = tenantDir.resolve(fileName);
            // 使用绝对路径的 File 对象
            file.transferTo(targetFile.toFile());

            log.debug("文件已上传至: {}", targetFile);

            // 返回相对路径，用于前端访问 (保持原样，WebMvcConfig 应该处理了 /uploads 的映射)
            String url = "/" + uploadPath + "/" + tenantCode + "/" + datePath + "/" + fileName;

            // 保存到数据库
            MediaAsset asset = new MediaAsset();
            asset.setName(originalFilename);
            asset.setUrl(url);
            asset.setFilePath(targetFile.toString());
            asset.setType(file.getContentType());
            asset.setSize(file.getSize());
            mediaAssetRepository.save(asset);

            return url;
        } catch (IOException e) {
            log.error("文件上传失败, targetDir: {}", tenantDir, e);
            throw new BusinessException("文件上传失败");
        }
    }

    private void validateMagicNumber(MultipartFile file, String extension) {
        // 对于文本类文件（.txt, .md, .svg），跳过二进制头校验
        if (Arrays.asList(".txt", ".md", ".svg").contains(extension)) {
            return;
        }

        try (InputStream is = file.getInputStream()) {
            byte[] head = new byte[4];
            if (is.read(head) != -1) {
                String hexHead = bytesToHex(head);
                log.debug("文件 [{}] 的 Magic Number: {}", file.getOriginalFilename(), hexHead);
                
                boolean match = false;
                for (Map.Entry<String, String> entry : MAGIC_NUMBERS.entrySet()) {
                    if (hexHead.startsWith(entry.getKey())) {
                        match = true;
                        break;
                    }
                }
                
                if (!match) {
                    log.warn("文件内容安全校验失败: 疑似伪造文件, filename={}, magicNumber={}", 
                            file.getOriginalFilename(), hexHead);
                    throw new BusinessException("文件安全校验失败: 内容与后缀不符");
                }
            }
        } catch (IOException e) {
            log.error("读取文件头失败", e);
            throw new BusinessException("文件安全校验失败: 无法读取内容");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MediaAssetDto> listMediaAssets(String name, String type, Boolean deleted, Pageable pageable) {
        Specification<MediaAsset> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 模糊查询名称
            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            
            // 类型查询
            if (StringUtils.hasText(type)) {
                if ("image".equalsIgnoreCase(type)) {
                    predicates.add(cb.like(root.get("type"), "image/%"));
                } else if ("video".equalsIgnoreCase(type)) {
                    predicates.add(cb.like(root.get("type"), "video/%"));
                } else if ("audio".equalsIgnoreCase(type)) {
                    predicates.add(cb.like(root.get("type"), "audio/%"));
                } else if ("document".equalsIgnoreCase(type)) {
                    predicates.add(cb.not(cb.or(
                        cb.like(root.get("type"), "image/%"),
                        cb.like(root.get("type"), "video/%"),
                        cb.like(root.get("type"), "audio/%")
                    )));
                }
            }
            
            // 是否在回收站
            if (deleted != null) {
                predicates.add(cb.equal(root.get("deleted"), deleted));
            } else {
                // 默认只查未删除的
                predicates.add(cb.equal(root.get("deleted"), false));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return mediaAssetRepository.findAll(spec, pageable)
                .map(BeanConverter::toDto);
    }

    @Override
    @Transactional
    public void moveAssetToTrash(Long id) {
        mediaAssetRepository.findById(id).ifPresent(asset -> {
            asset.setDeleted(true);
            asset.setDeleteTime(LocalDateTime.now());
            mediaAssetRepository.save(asset);
        });
    }

    @Override
    @Transactional
    public void restoreAssetFromTrash(Long id) {
        mediaAssetRepository.findById(id).ifPresent(asset -> {
            asset.setDeleted(false);
            asset.setDeleteTime(null);
            mediaAssetRepository.save(asset);
        });
    }

    @Override
    @Transactional
    public void permanentlyDeleteAsset(Long id) {
        mediaAssetRepository.findById(id).ifPresent(asset -> {
            // 先删除物理文件
            deletePhysicalFile(asset.getUrl());
            // 再删除数据库记录
            mediaAssetRepository.delete(asset);
        });
    }

    @Override
    @Transactional
    public void emptyTrash() {
        // 获取所有已删除的资源
        Specification<MediaAsset> spec = (root, query, cb) -> cb.equal(root.get("deleted"), true);
        List<MediaAsset> deletedAssets = mediaAssetRepository.findAll(spec);
        
        for (MediaAsset asset : deletedAssets) {
            deletePhysicalFile(asset.getUrl());
            mediaAssetRepository.delete(asset);
        }
    }

    private void deletePhysicalFile(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }

        Path path;
        if (url.startsWith("/")) {
            String relativePath = url.substring(1);
            path = Paths.get(relativePath).toAbsolutePath();
        } else {
            path = Paths.get(url).toAbsolutePath();
        }

        try {
            Files.deleteIfExists(path);
            log.debug("物理文件已删除: {}", path);
        } catch (IOException e) {
            log.error("物理文件删除失败: {}", url, e);
        }
    }
}
