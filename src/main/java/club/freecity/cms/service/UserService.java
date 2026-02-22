package club.freecity.cms.service;

import club.freecity.cms.security.CustomUserDetails;
import club.freecity.cms.converter.BeanConverter;
import club.freecity.cms.dto.PasswordUpdateDto;
import club.freecity.cms.dto.ProfileDto;
import club.freecity.cms.dto.UserDto;
import club.freecity.cms.entity.User;
import club.freecity.cms.enums.UserRole;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.repository.UserRepository;
import club.freecity.cms.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCacheService userCacheService;

    /**
     * 处理登录失败，增加失败计数并检查是否锁定
     * @return 更新后的失败次数，如果用户不存在则返回 0
     */
    @Transactional
    public int handleLoginFailure(String username, int maxFailCount, int lockoutMinutes) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return 0;

        user.setLoginFailCount(user.getLoginFailCount() + 1);
        if (user.getLoginFailCount() >= maxFailCount) {
            user.setLockoutTime(LocalDateTime.now().plusMinutes(lockoutMinutes));
        }
        userRepository.save(user);
        userCacheService.evictUserCache(user.getTenantId(), user.getUsername());
        return user.getLoginFailCount();
    }

    /**
     * 重置登录失败计数和锁定状态
     */
    @Transactional
    public void resetLoginFailure(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;

        if (user.getLoginFailCount() > 0 || user.getLockoutTime() != null) {
            user.setLoginFailCount(0);
            user.setLockoutTime(null);
            userRepository.save(user);
            userCacheService.evictUserCache(user.getTenantId(), user.getUsername());
        }
    }

    @Transactional(readOnly = true)
    public List<UserDto> listAllUsers() {
        return userRepository.findAll().stream()
                .map(BeanConverter::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return BeanConverter.toDto(user);
    }

    @Transactional
    public UserDto saveUser(UserDto userDto) {
        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new BusinessException("用户名已存在");
        }

        UserRole newRole = UserRole.fromValue(userDto.getRole());
        // 只有超级管理员可以创建超级管理员
        if (UserRole.SUPER_ADMIN.equals(newRole) && !isCurrentSuperAdmin()) {
            throw new BusinessException("无权创建超级管理员账户");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setNickname(userDto.getNickname());
        user.setEmail(userDto.getEmail());
        user.setAvatar(userDto.getAvatar());
        user.setRole(newRole);
        user.setStatus(userDto.getStatus() != null ? userDto.getStatus() : "ENABLED");
        user.setPasswordUpdateTime(LocalDateTime.now());
        
        return BeanConverter.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(UserDto userDto) {
        User user = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        UserRole newRole = UserRole.fromValue(userDto.getRole());
        // 只有超级管理员可以修改超级管理员账户，或者将普通账户提升为超级管理员
        if (UserRole.SUPER_ADMIN.equals(newRole) || UserRole.SUPER_ADMIN.equals(user.getRole())) {
            if (!isCurrentSuperAdmin()) {
                throw new BusinessException("无权操作超级管理员账户或提升角色为超级管理员");
            }
        }

        BeanConverter.updateEntity(user, userDto);
        
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setPasswordUpdateTime(LocalDateTime.now());
        }
        
        User savedUser = userRepository.save(user);
        userCacheService.evictUserCache(savedUser.getTenantId(), savedUser.getUsername());
        return BeanConverter.toDto(savedUser);
    }

    @Transactional
    public UserDto updateUserProfile(Long userId, ProfileDto profileDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        user.setNickname(profileDto.getNickname());
        user.setEmail(profileDto.getEmail());
        user.setAvatar(profileDto.getAvatar());
        user.setBio(profileDto.getBio()); 
        
        User savedUser = userRepository.save(user);
        userCacheService.evictUserCache(savedUser.getTenantId(), savedUser.getUsername());
        return BeanConverter.toDto(savedUser);
    }

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateDto passwordUpdateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        if (!passwordEncoder.matches(passwordUpdateDto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        
        // 校验新密码强度
        PasswordUtils.validate(passwordUpdateDto.getNewPassword());
        
        user.setPassword(passwordEncoder.encode(passwordUpdateDto.getNewPassword()));
        user.setPasswordUpdateTime(LocalDateTime.now());
        userRepository.save(user);
        userCacheService.evictUserCache(user.getTenantId(), user.getUsername());
    }

    @Transactional
    public void deleteSelf(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        if (UserRole.SUPER_ADMIN.equals(user.getRole())) {
            // 超级管理员不能注销自己? 或者至少要保留一个超级管理员?
            // 简单起见，暂不允许超级管理员注销自己
            throw new BusinessException("超级管理员不能注销账户");
        }

        if (UserRole.TENANT_ADMIN.equals(user.getRole())) {
            // 租户管理员不能注销自己? 或者至少要保留一个租户管理员?
            // 简单起见，暂不允许租户管理员注销自己
            throw new BusinessException("租户管理员不能注销账户");
        }
        
        userRepository.deleteById(userId);
        userCacheService.evictUserCache(user.getTenantId(), user.getUsername());
    }

    private boolean isCurrentSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return UserRole.SUPER_ADMIN.getValue().equals(userDetails.getRole());
        }
        return false;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 禁止删除超级管理员
        if (UserRole.SUPER_ADMIN.equals(user.getRole())) {
            throw new BusinessException("禁止删除超级管理员账户");
        }

        // 租户下的所有用户无权删除租户管理员（只有超级管理员可以删除租户管理员）
        if (UserRole.TENANT_ADMIN.equals(user.getRole()) && !isCurrentSuperAdmin()) {
            throw new BusinessException("无权删除租户管理员账户");
        }
        
        userRepository.deleteById(id);
        userCacheService.evictUserCache(user.getTenantId(), user.getUsername());
    }
}
