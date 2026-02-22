package club.freecity.cms.tenant;

import club.freecity.cms.enums.TenantStatus;
import club.freecity.cms.common.TenantContext;
import club.freecity.cms.entity.Tenant;
import club.freecity.cms.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 确保在 Spring Security 之前执行，或在 JWT 之前
public class TenantIdentificationFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Value("${app.tenant.base-domain}")
    private String baseDomain;

    @Value("${app.tenant.default-code}")
    private String defaultCode;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 排除静态资源路径，减少数据库查询
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/img/") || 
               path.startsWith("/fonts/") || 
               path.startsWith("/favicon.ico") ||
               path.startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String host = request.getServerName();
        String tenantCode = extractTenantCode(host);

        try {
            Optional<Tenant> tenantOpt = tenantRepository.findByCode(tenantCode);
            if (tenantOpt.isPresent()) {
                Tenant tenant = tenantOpt.get();
                // 检查租户状态
                if (!TenantStatus.ACTIVE.getValue().equalsIgnoreCase(tenant.getStatus())) {
                    log.warn("Tenant is disabled: {} (Status: {})", tenantCode, tenant.getStatus());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant is disabled");
                    return;
                }
                
                // 检查租户是否过期
                if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
                    log.warn("Tenant is expired: {} (Expire Time: {})", tenantCode, tenant.getExpireTime());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant service has expired");
                    return;
                }

                TenantContext.setCurrentTenant(tenant.getId(), tenant.getCode());
                log.debug("Identified tenant: {} (ID: {}) from host: {}", tenantCode, tenant.getId(), host);
                filterChain.doFilter(request, response);
            } else {
                log.warn("Unknown tenant code: {} from host: {}", tenantCode, host);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found: " + tenantCode);
            }
        } catch (Exception e) {
            log.error("租户识别失败: host={}, code={}", host, tenantCode, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 500, \"message\": \"系统初始化中或数据库连接失败，请检查后端日志\"}");
        } finally {
            // 注意：如果是 JwtAuthenticationFilter 之后执行，这里可能会清除 JWT 设置的租户
            // 但因为我们设置了 HIGHEST_PRECEDENCE + 1，它通常在 Security 链之前运行
            // 如果是在 Security 内部运行，则需要更精细的控制
            // 这里我们暂时保留，后续在 SecurityConfig 中调整顺序
            TenantContext.clear();
        }
    }

    private String extractTenantCode(String host) {
        if (host == null || host.equalsIgnoreCase(baseDomain) || host.equalsIgnoreCase("www." + baseDomain)) {
            return defaultCode;
        }

        if (host.endsWith("." + baseDomain)) {
            return host.substring(0, host.length() - baseDomain.length() - 1);
        }

        // 处理 localhost 或其他情况，默认返回 admin 或者解析第一段
        // 如果是 IP 地址，直接返回默认租户
        if (host.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            return defaultCode;
        }

        int firstDot = host.indexOf(".");
        if (firstDot > 0) {
            return host.substring(0, firstDot);
        }

        return defaultCode;
    }
}
