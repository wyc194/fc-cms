package club.freecity.cms.service;

import club.freecity.cms.common.SecurityConstants;
import club.freecity.cms.common.TenantContext;
import club.freecity.cms.security.CustomUserDetails;
import club.freecity.cms.dto.AuthResponseDto;
import club.freecity.cms.dto.LoginDto;
import club.freecity.cms.entity.User;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.UserRepository;
import club.freecity.cms.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    private static final int MAX_LOGIN_FAIL_COUNT = 3;
    private static final int LOCKOUT_MINUTES = 5;

    public AuthResponseDto login(LoginDto loginDto) {
        User user = userRepository.findByUsername(loginDto.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        // 检查是否被锁定
        if (user.getLockoutTime() != null) {
            if (user.getLockoutTime().isAfter(LocalDateTime.now())) {
                long minutesLeft = Duration.between(LocalDateTime.now(), user.getLockoutTime()).toMinutes();
                throw new BusinessException("账号已锁定，请在 " + (minutesLeft + 1) + " 分钟后再试");
            } else {
                // 锁定时间已过，重置（使用独立事务）
                userService.resetLoginFailure(user.getUsername());
            }
        }

        try {
            log.debug("Attempting login for user: {}", loginDto.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            // 登录成功，重置失败次数（使用独立事务，确保即使后续 token 生成等操作异常，重置也能生效）
            userService.resetLoginFailure(user.getUsername());

            String accessToken = jwtUtils.generateAccessToken(
                    userDetails.getUserId(), 
                    userDetails.getUsername(), 
                    userDetails.getRole(), 
                    userDetails.getTenantId(),
                    userDetails.getTenantCode(),
                    userDetails.getPasswordUpdateTime()
            );

            String refreshToken = jwtUtils.generateRefreshToken(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getRole(),
                    userDetails.getTenantId(),
                    userDetails.getTenantCode(),
                    userDetails.getPasswordUpdateTime()
            );

            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(AuthResponseDto.UserDto.builder()
                            .id(userDetails.getUserId())
                            .username(userDetails.getUsername())
                            .nickname(user.getNickname())
                            .avatar(user.getAvatar())
                            .role(userDetails.getRole())
                            .tenantId(userDetails.getTenantId())
                            .build())
                    .build();
        } catch (BadCredentialsException e) {
            // 登录失败，增加计数（通过新事务提交，防止异常回滚）
            int failCount = userService.handleLoginFailure(loginDto.getUsername(), MAX_LOGIN_FAIL_COUNT, LOCKOUT_MINUTES);
            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                throw new BusinessException("密码错误次数过多，账号已锁定 " + LOCKOUT_MINUTES + " 分钟");
            }
            int remainingAttempts = MAX_LOGIN_FAIL_COUNT - failCount;
            throw new BusinessException("用户名或密码错误，还剩 " + remainingAttempts + " 次机会");
        } catch (AuthenticationException e) {
            log.error("认证失败: {}", e.getMessage());
            throw new BusinessException("认证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("登录系统异常", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public AuthResponseDto refreshToken(String refreshToken) {
        try {
            String username = jwtUtils.extractUsername(refreshToken);
            String type = jwtUtils.extractType(refreshToken);
            
            if (!SecurityConstants.TOKEN_TYPE_REFRESH.equals(type)) {
                throw new BusinessException("无效的 Refresh Token");
            }

            // 设置租户上下文，以便查询用户信息
            Long tenantId = jwtUtils.extractTenantId(refreshToken);
            String tenantCode = jwtUtils.extractTenantCode(refreshToken);
            if (tenantId != null) {
                club.freecity.cms.common.TenantContext.setCurrentTenant(tenantId, tenantCode);
            }

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException("用户不存在"));

            // 校验密码更新时间，如果 Refresh Token 中的时间早于数据库中的时间，说明密码已更改，Refresh Token 失效
            Long tokenPwdUpdateTime = jwtUtils.extractPasswordUpdateTime(refreshToken);
            if (user.getPasswordUpdateTime() != null) {
                long dbTimeMillis = user.getPasswordUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (tokenPwdUpdateTime == null || tokenPwdUpdateTime < dbTimeMillis - 1000) {
                    throw new BusinessException("密码已更改，请重新登录");
                }
            }

            CustomUserDetails userDetails = new CustomUserDetails(user);

            String newAccessToken = jwtUtils.generateAccessToken(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getRole(),
                    userDetails.getTenantId(),
                    userDetails.getTenantCode(),
                    userDetails.getPasswordUpdateTime()
            );

            String newRefreshToken = jwtUtils.generateRefreshToken(
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getRole(),
                    userDetails.getTenantId(),
                    userDetails.getTenantCode(),
                    userDetails.getPasswordUpdateTime()
            );

            return AuthResponseDto.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .user(AuthResponseDto.UserDto.builder()
                            .id(userDetails.getUserId())
                            .username(userDetails.getUsername())
                            .nickname(user.getNickname())
                            .avatar(user.getAvatar())
                            .role(userDetails.getRole())
                            .tenantId(userDetails.getTenantId())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Token 刷新失败", e);
            throw new BusinessException("Token 刷新失败: " + e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
