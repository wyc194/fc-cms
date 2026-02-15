package club.freecity.cms.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MediaAssetDto {
    private Long id;
    private String name;
    private String url;
    private String type;
    private Long size;
    private Boolean deleted;
    private LocalDateTime deleteTime;
    private LocalDateTime createTime;
}
