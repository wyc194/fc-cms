package club.freecity.cms.service.impl;

import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.security.CustomUserDetails;
import club.freecity.cms.dto.DashboardStatsDto;
import club.freecity.cms.enums.TenantStatus;
import club.freecity.cms.repository.ArticleRepository;
import club.freecity.cms.repository.PackageRepository;
import club.freecity.cms.repository.TenantRepository;
import club.freecity.cms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ArticleRepository articleRepository;
    private final TenantRepository tenantRepository;
    private final PackageRepository packageRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getStatistics() {
        DashboardStatsDto.DashboardStatsDtoBuilder builder = DashboardStatsDto.builder();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (RoleConstants.SUPER_ADMIN.equals(userDetails.getRole())) {
                // Super Admin stats
                builder.tenantCount(tenantRepository.count());
                builder.activeTenantCount(tenantRepository.countByStatus(TenantStatus.ACTIVE.getValue()));
                builder.packageCount(packageRepository.count());
                builder.totalPlatformArticleCount(articleRepository.count());
            } else {
                // Regular Admin/Editor stats (filtered by tenant automatically by TenantAspect)
                builder.articleCount(articleRepository.count());
                builder.publishedCount(articleRepository.countByPublishedTrue());
                builder.draftCount(articleRepository.count() - articleRepository.countByPublishedTrue());
                
                Long totalViews = articleRepository.sumViewCount();
                builder.todayViewCount(totalViews != null ? totalViews : 0L);
            }
        }

        return builder.build();
    }
}
