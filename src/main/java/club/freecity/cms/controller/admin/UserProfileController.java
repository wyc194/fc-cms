package club.freecity.cms.controller.admin;

import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.security.CustomUserDetails;
import club.freecity.cms.dto.PasswordUpdateDto;
import club.freecity.cms.dto.ProfileDto;
import club.freecity.cms.dto.UserDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping
    public Result<UserDto> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return Result.success(userService.getUserById(userDetails.getUserId()));
    }

    @PutMapping
    @SecurityAudit(action = AuditAction.USER_PROFILE_UPDATE, message = "'更新个人资料'")
    public Result<UserDto> updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails, 
                                         @RequestBody @Validated ProfileDto profileDto) {
        return Result.success(userService.updateUserProfile(userDetails.getUserId(), profileDto));
    }

    @PutMapping("/password")
    @SecurityAudit(action = AuditAction.USER_PASSWORD_UPDATE, message = "'修改个人密码'")
    public Result<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestBody @Validated PasswordUpdateDto passwordUpdateDto) {
        userService.updatePassword(userDetails.getUserId(), passwordUpdateDto);
        return Result.success();
    }

    @DeleteMapping
    @SecurityAudit(action = AuditAction.USER_SELF_DELETE, message = "'注销个人账户'")
    public Result<Void> deleteAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteSelf(userDetails.getUserId());
        return Result.success();
    }
}
