package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.PackageDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    public Result<List<PackageDto>> listPackages() {
        return Result.success(packageService.listAllPackages());
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    public Result<PackageDto> getPackageById(@PathVariable Long id) {
        return Result.success(packageService.getPackageById(id));
    }

    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    @SecurityAudit(action = AuditAction.PACKAGE_CREATE, message = "'创建套餐: ' + #packageDto.name", logArgs = true)
    public Result<PackageDto> savePackage(@RequestBody PackageDto packageDto) {
        return Result.success(packageService.savePackage(packageDto));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    @SecurityAudit(action = AuditAction.PACKAGE_UPDATE, message = "'更新套餐 ID: ' + #packageDto.id", logArgs = true)
    public Result<PackageDto> updatePackage(@RequestBody PackageDto packageDto) {
        return Result.success(packageService.updatePackage(packageDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ROLE_SUPER_ADMIN)
    @SecurityAudit(action = AuditAction.PACKAGE_DELETE, message = "'删除套餐 ID: ' + #id")
    public Result<Void> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return Result.success();
    }
}
