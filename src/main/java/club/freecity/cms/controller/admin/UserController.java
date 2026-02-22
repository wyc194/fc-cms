package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.common.RoleConstants;
import club.freecity.cms.dto.UserDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.service.UserService;
import club.freecity.cms.validator.group.CreateGroup;
import club.freecity.cms.validator.group.UpdateGroup;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    public Result<List<UserDto>> listUsers() {
        return Result.success(userService.listAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    public Result<UserDto> getUserById(@PathVariable @NotNull Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.USER_CREATE, message = "'创建新用户: ' + #userDto.username", logArgs = true)
    public Result<UserDto> saveUser(@Validated(CreateGroup.class) @RequestBody UserDto userDto) {
        return Result.success(userService.saveUser(userDto));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.USER_UPDATE, message = "'更新用户 ID: ' + #userDto.id", logArgs = true)
    public Result<UserDto> updateUser(@Validated(UpdateGroup.class) @RequestBody UserDto userDto) {
        return Result.success(userService.updateUser(userDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ANY_ROLE_ADMIN)
    @SecurityAudit(action = AuditAction.USER_DELETE, message = "'删除用户 ID: ' + #id")
    public Result<Void> deleteUser(@PathVariable @NotNull Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
