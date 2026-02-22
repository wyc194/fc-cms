package club.freecity.cms.controller.api;

import club.freecity.cms.annotation.RateLimit;
import club.freecity.cms.annotation.SecurityAudit;
import club.freecity.cms.common.Result;
import club.freecity.cms.dto.AuthResponseDto;
import club.freecity.cms.dto.LoginDto;
import club.freecity.cms.dto.RefreshDto;
import club.freecity.cms.enums.AuditAction;
import club.freecity.cms.enums.LimitType;
import club.freecity.cms.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @RateLimit(count = 3, window = 60, limitType = LimitType.IP, key = "login_limit:")
    @SecurityAudit(action = AuditAction.AUTH_LOGIN, message = "'用户 ' + #loginDto.username + ' 尝试登录'", logArgs = true)
    public Result<AuthResponseDto> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
        AuthResponseDto response = authService.login(loginDto);
        return Result.success(response);
    }

    @PostMapping("/refresh")
    @SecurityAudit(action = AuditAction.AUTH_REFRESH, message = "'刷新令牌'")
    public Result<AuthResponseDto> refresh(@RequestBody RefreshDto refreshDto) {
        return Result.success(authService.refreshToken(refreshDto.getRefreshToken()));
    }
}