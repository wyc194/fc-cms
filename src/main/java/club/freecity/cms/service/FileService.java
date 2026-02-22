package club.freecity.cms.service;

import club.freecity.cms.dto.MediaAssetDto;
import club.freecity.cms.dto.StorageStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    /**
     * 获取存储统计信息
     * @return 存储统计DTO
     */
    StorageStatsDto getStorageStats();

    /**
     * 上传文件并返回访问路径
     * @param file 文件对象
     * @return 文件的相对访问路径
     */
    String uploadFile(MultipartFile file);

    /**
     * 获取媒体资源列表
     * @param name 模糊查询名称
     * @param type 类型查询 (image, video, audio, document 等)
     * @param deleted 是否查询回收站中的资源
     * @param pageable 分页信息
     * @return 媒体资源分页列表
     */
    Page<MediaAssetDto> listMediaAssets(String name, String type, Boolean deleted, Pageable pageable);

    /**
     * 将资源移至回收站
     * @param id 资源ID
     */
    void moveAssetToTrash(Long id);

    /**
     * 从回收站恢复资源
     * @param id 资源ID
     */
    void restoreAssetFromTrash(Long id);

    /**
     * 物理删除文件及其记录
     * @param id 资源ID
     */
    void permanentlyDeleteAsset(Long id);

    /**
     * 清空回收站
     */
    void emptyTrash();
}
