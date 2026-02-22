package club.freecity.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageStatsDto {
    private Long usedSize;      // 已使用空间 (Bytes)
    private Long totalSize;     // 总容量 (Bytes)
    private Double percentage;  // 使用百分比
}
