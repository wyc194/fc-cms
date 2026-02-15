package club.freecity.cms.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import club.freecity.cms.security.JwtAuthenticationFilter;
import club.freecity.cms.common.Result;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 对于使用 JWT 的无状态 API，通常禁用 CSRF。
            // 因为 JWT 是通过 Authorization Header 传输，而非 Cookie，所以不受 CSRF 攻击影响。
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .headers(headers -> headers
                // 开启 HSTS (HTTP Strict Transport Security)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                // // 暂时禁用 HSTS，防止本地开发时强制跳转 HTTPS 导致无法访问 HTTP 开发服务器
                // .httpStrictTransportSecurity(hsts -> hsts.disable())
                // 防止点击劫持
                .frameOptions(frame -> frame.deny())
                // 明确开启 X-Content-Type-Options: nosniff，防御 MIME 嗅探攻击（实际Spring Security哲学是“默认安全”，开启.headers()配置即默认加上）
                .contentTypeOptions(Customizer.withDefaults())
                // 启用内容安全策略 (CSP) - 基础配置，已放行 cdn.jsdelivr.net, cdnjs.cloudflare.com, jinrishici.com 等公共资源
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' cdn.jsdelivr.net cdnjs.cloudflare.com sdk.jinrishici.com; style-src 'self' 'unsafe-inline' cdn.jsdelivr.net cdnjs.cloudflare.com; img-src 'self' data: *; font-src 'self' data: cdn.jsdelivr.net cdnjs.cloudflare.com; connect-src 'self' cdn.jsdelivr.net v2.jinrishici.com;")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll() // 登录接口公开
                .requestMatchers("/api/admin/**").authenticated() // 管理端接口需要认证
                .anyRequest().permitAll() // 博客前台页面等公开
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            configuration.setAllowedOrigins(Arrays.asList("*"));
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Tenant-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            log.warn("Unauthorized access: {}", authException.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Result<Void> result = Result.error(401, "未登录或登录已过期");
            try (PrintWriter writer = response.getWriter()) {
                writer.write(objectMapper.writeValueAsString(result));
                writer.flush();
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.warn("Access denied: {}", accessDeniedException.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            Result<Void> result = Result.error(403, "权限不足，无法访问");
            try (PrintWriter writer = response.getWriter()) {
                writer.write(objectMapper.writeValueAsString(result));
                writer.flush();
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        log.debug("Creating DaoAuthenticationProvider with userDetailsService: {}", userDetailsService);
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 建议实际使用 BCrypt，这里为了兼容已有数据，你可以先用 NoOp 或者统一升级数据
        return new BCryptPasswordEncoder();
    }
}
