package club.freecity.cms.security;

import club.freecity.cms.common.TenantContext;
import club.freecity.cms.util.JwtUtils;
import club.freecity.cms.common.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            username = jwtUtils.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 在加载用户信息前，先从 JWT 中提取租户信息并设置到上下文
                // 这样 UserDetailsService 中的 Repository 查询才能正确匹配到租户
                Long tenantId = jwtUtils.extractTenantId(jwt);
                String tenantCode = jwtUtils.extractTenantCode(jwt);
                if (tenantId != null) {
                    TenantContext.setCurrentTenant(tenantId, tenantCode);
                }

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                // 检查用户状态是否启用
                if (!userDetails.isEnabled()) {
                    // 如果用户已禁用，不进行认证，直接跳过（后续由 Spring Security 抛出 401/403）
                    filterChain.doFilter(request, response);
                    return;
                }

                // 校验 Token 有效性，并确保类型为 access
                if (jwtUtils.validateToken(jwt, userDetails.getUsername()) && SecurityConstants.TOKEN_TYPE_ACCESS.equals(jwtUtils.extractType(jwt))) {
                    // 校验密码版本/更新时间，如果 Token 中的时间早于数据库中的时间，说明密码已更改，Token 失效
                    if (userDetails instanceof CustomUserDetails customUserDetails) {
                        Long tokenPwdUpdateTime = jwtUtils.extractPasswordUpdateTime(jwt);
                        LocalDateTime dbPwdUpdateTime = customUserDetails.getPasswordUpdateTime();
                        
                        if (dbPwdUpdateTime != null) {
                            long dbTimeMillis = dbPwdUpdateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                            // 允许 1 秒以内的误差，防止由于存储精度导致的误判
                            if (tokenPwdUpdateTime == null || tokenPwdUpdateTime < dbTimeMillis - 1000) {
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 重新确保租户上下文是最新的（来自数据库加载的信息）
                    if (userDetails instanceof CustomUserDetails) {
                        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                        TenantContext.setCurrentTenant(customUserDetails.getTenantId(), customUserDetails.getTenantCode());
                    }
                }
            }
        } catch (Exception e) {
            // Token 验证失败，不设置 SecurityContext
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 不再这里清理，由最外层的 TenantIdentificationFilter 统一清理
            // 否则会清除掉子域名识别到的租户信息
        }
    }
}
