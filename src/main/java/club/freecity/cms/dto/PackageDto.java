package club.freecity.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Double price;
    private Integer maxArticles;
    private Long maxStorage;
    private Boolean customDomainEnabled;
    private Boolean advancedStatsEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
