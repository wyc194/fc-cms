package club.freecity.cms.service;

import club.freecity.cms.security.CustomUserDetails;
import club.freecity.cms.dto.AuthResponseDto;
import club.freecity.cms.dto.LoginDto;
import club.freecity.cms.entity.User;
import club.freecity.cms.enums.UserRole;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.UserRepository;
import club.freecity.cms.util.JwtUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginDto loginDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setNickname("Test Nickname");
        testUser.setRole(UserRole.SUPER_ADMIN);
        testUser.setTenantId(1L);

        loginDto = new LoginDto();
        loginDto.setUsername("testuser");
        loginDto.setPassword("password");
    }

    @Test
    @DisplayName("登录成功 - 验证 Token 生成和失败计数重置")
    void login_Success() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyString(), anyLong(), any(), any()))
                .thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyString(), anyLong(), any(), any()))
                .thenReturn("refresh-token");

        // Act
        AuthResponseDto response = authService.login(loginDto);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");
        
        verify(userService).resetLoginFailure("testuser");
        verify(jwtUtils, times(1)).generateAccessToken(anyLong(), anyString(), anyString(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("登录失败 - 密码错误 - 验证失败计数增加")
    void login_BadCredentials() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userService.handleLoginFailure(anyString(), anyInt(), anyInt())).thenReturn(1);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还剩 2 次机会");

        verify(userService).handleLoginFailure(eq("testuser"), anyInt(), anyInt());
    }

    @Test
    @DisplayName("登录失败 - 账户已锁定")
    void login_AccountLocked() {
        // Arrange
        testUser.setLockoutTime(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已锁定");

        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("登录失败 - 达到最大错误次数导致锁定")
    void login_MaxFailuresReached() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userService.handleLoginFailure(anyString(), anyInt(), anyInt())).thenReturn(3);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号已锁定 5 分钟");
    }
}
