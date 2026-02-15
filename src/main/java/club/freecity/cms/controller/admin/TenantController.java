package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.TenantCreateResultDto;
import club.freecity.cms.dto.TenantDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.service.TenantService;
import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@Validated
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    public Result<List<TenantDto>> listTenants() {
        return Result.success(tenantService.listAllTenants());
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    public Result<TenantDto> getTenantById(@PathVariable @NotNull Long id) {
        return Result.success(tenantService.getTenantById(id));
    }

    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    @SecurityAudit(action = AuditAction.TENANT_CREATE, message = "'创建新租户: ' + #tenantDto.name + '(' + #tenantDto.code + ')'", logArgs = true)
    public Result<TenantCreateResultDto> saveTenant(@Validated(CreateGroup.class) @RequestBody TenantDto tenantDto) {
        return Result.success(tenantService.saveTenant(tenantDto));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    @SecurityAudit(action = AuditAction.TENANT_UPDATE, message = "'更新租户 ID: ' + #tenantDto.id", logArgs = true)
    public Result<TenantDto> updateTenant(@Validated(UpdateGroup.class) @RequestBody TenantDto tenantDto) {
        return Result.success(tenantService.updateTenant(tenantDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    @SecurityAudit(action = AuditAction.TENANT_DELETE, message = "'删除租户 ID: ' + #id")
    public Result<Void> deleteTenant(@PathVariable @NotNull Long id) {
        tenantService.deleteTenant(id);
        return Result.success();
    }

    @GetMapping("/config")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    public Result<TenantDto> getCurrentConfig() {
        return Result.success(tenantService.getCurrentTenantConfig());
    }

    @PutMapping("/config")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.TENANT_CONFIG_UPDATE, message = "'更新当前租户配置'", logArgs = true)
    public Result<TenantDto> updateCurrentConfig(@Validated(UpdateGroup.class) @RequestBody TenantDto tenantDto) {
        // 确保只能更新当前租户
        Long currentTenantId = club.freecity.cms.common.TenantContext.getCurrentTenantId();
        tenantDto.setId(currentTenantId);
        return Result.success(tenantService.updateTenant(tenantDto));
    }
}
