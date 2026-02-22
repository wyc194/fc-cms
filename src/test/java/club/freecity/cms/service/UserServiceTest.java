package club.freecity.cms.service;

import club.freecity.cms.dto.UserDto;
import club.freecity.cms.entity.User;
import club.freecity.cms.enums.UserRole;
import club.freecity.cms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserCacheService userCacheService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setLoginFailCount(0);
        testUser.setRole(UserRole.TENANT_ADMIN);
        testUser.setTenantId(1L);
    }

    @Test
    @DisplayName("处理登录失败 - 增加失败计数")
    void handleLoginFailure_IncrementCount() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act
        int failCount = userService.handleLoginFailure("testuser", 3, 5);
        
        // Assert
        assertThat(failCount).isEqualTo(1);
        assertThat(testUser.getLoginFailCount()).isEqualTo(1);
        verify(userRepository).save(testUser);
        verify(userCacheService).evictUserCache(any(), any());
    }

    @Test
    @DisplayName("处理登录失败 - 达到最大次数锁定账户")
    void handleLoginFailure_LockAccount() {
        // Arrange
        testUser.setLoginFailCount(2);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act
        int failCount = userService.handleLoginFailure("testuser", 3, 5);
        
        // Assert
        assertThat(failCount).isEqualTo(3);
        assertThat(testUser.getLockoutTime()).isNotNull();
        assertThat(testUser.getLockoutTime()).isAfter(LocalDateTime.now());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("重置登录失败 - 清除计数和锁定状态")
    void resetLoginFailure_ClearStatus() {
        // Arrange
        testUser.setLoginFailCount(2);
        testUser.setLockoutTime(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // Act
        userService.resetLoginFailure("testuser");
        
        // Assert
        assertThat(testUser.getLoginFailCount()).isZero();
        assertThat(testUser.getLockoutTime()).isNull();
        verify(userRepository).save(testUser);
        verify(userCacheService).evictUserCache(any(), any());
    }

    @Test
    @DisplayName("创建用户 - 成功")
    void saveUser_Success() {
        // Arrange
        UserDto newUserDto = new UserDto();
        newUserDto.setUsername("newuser");
        newUserDto.setPassword("password123");
        newUserDto.setRole(UserRole.VIEWER.getValue());
        
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // Act
        UserDto result = userService.saveUser(newUserDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userRepository).save(any(User.class));
    }
}
