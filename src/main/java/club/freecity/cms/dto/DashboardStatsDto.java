package club.freecity.cms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsDto {
    // Regular Admin Stats
    private Long articleCount;
    private Long publishedCount;
    private Long draftCount;
    private Long todayViewCount;

    // Super Admin Stats
    private Long tenantCount;
    private Long activeTenantCount;
    private Long packageCount;
    private Long totalPlatformArticleCount;
}
