package club.freecity.cms.security;

import club.freecity.cms.common.TenantContext;
import club.freecity.cms.entity.User;
import club.freecity.cms.repository.UserRepository;
import club.freecity.cms.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_USER_DETAILS, key = "T(club.freecity.cms.common.TenantContext).getCurrentTenantId() + ':' + #username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}, current tenant: {}", username, TenantContext.getCurrentTenantId());
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {} for tenant: {}", username, TenantContext.getCurrentTenantId());
                    return new UsernameNotFoundException("User not found with username: " + username);
                });
        
        log.debug("User found: {}, role: {}, tenantId: {}, password hash: {}", user.getUsername(), user.getRole(), user.getTenantId(), user.getPassword());

        return new CustomUserDetails(user);
    }
}
