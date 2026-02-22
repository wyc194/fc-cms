package club.freecity.cms.security;

import club.freecity.cms.common.SecurityConstants;
import club.freecity.cms.entity.User;
import club.freecity.cms.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    @Getter
    private final Long userId;
    @Getter
    private final String username;
    @Getter
    private final String password;
    @Getter
    private final String role;
    @Getter
    private final Long tenantId;
    @Getter
    private final String tenantCode;
    @Getter
    private final java.time.LocalDateTime passwordUpdateTime;
    
    private final String status;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole().getValue();
        this.tenantId = user.getTenantId();
        this.tenantCode = user.getTenant() != null ? user.getTenant().getCode() : null;
        this.passwordUpdateTime = user.getPasswordUpdateTime();
        this.status = user.getStatus();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.ENABLED.getValue().equalsIgnoreCase(status);
    }
}
