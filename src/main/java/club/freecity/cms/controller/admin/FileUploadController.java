package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.RateLimit;
import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.dto.MediaAssetDto;
import club.freecity.cms.dto.StorageStatsDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.enums.LimitType;
import club.freecity.cms.service.FileService;
import club.freecity.cms.common.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileService fileService;

    @GetMapping("/stats")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<StorageStatsDto> getStats() {
        return Result.success(fileService.getStorageStats());
    }

    @RateLimit(window = 60, count = 3, limitType = LimitType.USER)
    @PostMapping("/upload")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.FILE_UPLOAD, message = "'上传文件: ' + #file.originalFilename")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    @GetMapping("/list")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<Page<MediaAssetDto>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "false") Boolean deleted,
            @PageableDefault(sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return Result.success(fileService.listMediaAssets(name, type, deleted, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.FILE_DELETE, message = "'将文件移至回收站 ID: ' + #id")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.moveAssetToTrash(id);
        return Result.success();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.FILE_RESTORE, message = "'还原文件 ID: ' + #id")
    public Result<Void> restore(@PathVariable Long id) {
        fileService.restoreAssetFromTrash(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.FILE_DELETE, message = "'永久删除文件 ID: ' + #id")
    public Result<Void> permanentDelete(@PathVariable Long id) {
        fileService.permanentlyDeleteAsset(id);
        return Result.success();
    }

    @DeleteMapping("/trash/empty")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    @SecurityAudit(action = AuditAction.FILE_TRASH_EMPTY, message = "'清空回收站'")
    public Result<Void> emptyTrash() {
        fileService.emptyTrash();
        return Result.success();
    }
}
