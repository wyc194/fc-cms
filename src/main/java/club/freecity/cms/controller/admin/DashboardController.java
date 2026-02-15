package club.freecity.cms.controller.admin;

import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.DashboardStatsDto;
import club.freecity.cms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_EDITOR)
    public Result<DashboardStatsDto> getStats() {
        return Result.success(dashboardService.getStatistics());
    }
}
