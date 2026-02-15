package club.freecity.cms.service.impl;

import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.MediaAssetRepository;
import club.freecity.cms.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private MediaAssetRepository mediaAssetRepository;
    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "uploadPath", "uploads");
        ReflectionTestUtils.setField(fileService, "maxFileSize", DataSize.ofMegabytes(10));
    }

    @Test
    @DisplayName("上传文件 - 空文件抛出异常")
    void uploadFile_EmptyFile_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件不能为空");
    }

    @Test
    @DisplayName("上传文件 - 文件超过 10MB 抛出异常")
    void uploadFile_FileSizeExceeded_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(11 * 1024 * 1024L);

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件大小超过限制");
    }

    @Test
    @DisplayName("上传文件 - 不支持的后缀名抛出异常")
    void uploadFile_InvalidExtension_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.exe");

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件格式");
    }

    @Test
    @DisplayName("上传文件 - 不支持的 MIME 类型抛出异常")
    void uploadFile_InvalidMimeType_ThrowsException() {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("application/x-msdownload");

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");
    }

    @Test
    @DisplayName("上传文件 - Magic Number 校验失败抛出异常")
    void uploadFile_InvalidMagicNumber_ThrowsException() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        // 提供错误的 Magic Number (不是 FFD8FF)
        byte[] content = new byte[]{0x00, 0x00, 0x00, 0x00};
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        // Act & Assert
        assertThatThrownBy(() -> fileService.uploadFile(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件安全校验失败: 内容与后缀不符");
    }

    @Test
    @DisplayName("上传文件 - 文本类文件跳过 Magic Number 校验")
    void uploadFile_TextFile_SkipsMagicNumber() throws IOException {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("test.md");
        when(file.getContentType()).thenReturn("text/markdown");
        
        // 注意：这里由于后面涉及文件写入，直接运行会失败，但我们主要验证它通过了 validateMagicNumber
        // 由于没有 mock 租户上下文，会抛出后续的异常或在文件写入处失败，
        // 我们通过 verify 确认没有调用 getInputStream 来间接证明跳过了校验
        try {
            fileService.uploadFile(file);
        } catch (Exception ignored) {
        }

        verify(file, never()).getInputStream();
    }
}
