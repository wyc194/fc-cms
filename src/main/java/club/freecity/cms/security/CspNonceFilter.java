package club.freecity.cms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 内容安全策略（CSP）nonce 过滤器。
 * <p>
 * 为每个请求生成一次性 nonce 并下发 Content-Security-Policy 头：
 * <ul>
 *   <li>Thymeleaf 模板页面：script-src 使用 'nonce-xxx'，移除 'unsafe-inline'，
 *       使攻击者注入的内联脚本（如 XSS 实体走私复活的 onerror 处理器）无法执行；</li>
 *   <li>静态工具页（/tools*）与站点验证页：静态 HTML 无法模板化注入 nonce，
 *       回退到原先含 'unsafe-inline' 的旧策略。</li>
 * </ul>
 * nonce 同时写入 request attribute（{@link #CSP_NONCE_ATTRIBUTE}），
 * 由 {@code CspNonceAdvice} 以 ${cspNonce} 暴露给模板。
 */
@Component
public class CspNonceFilter extends OncePerRequestFilter {

    /** Thymeleaf 通过该 request attribute 读取 nonce */
    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";

    private static final String SCRIPT_HOSTS = "cdn.jsdelivr.net cdnjs.cloudflare.com sdk.jinrishici.com hm.baidu.com";
    private static final String CDN_HOSTS = "cdn.jsdelivr.net cdnjs.cloudflare.com";
    private static final String POLICY_SUFFIX =
            "style-src 'self' 'unsafe-inline' " + CDN_HOSTS + "; img-src 'self' data: *; font-src 'self' data: "
                    + CDN_HOSTS + "; connect-src 'self' cdn.jsdelivr.net v2.jinrishici.com;";

    private final SecureRandom random = new SecureRandom();

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // 错误页（/error）不含内联脚本，跳过注入可避免模板兜底脚本被拦截
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isNonceExemptPath(uri)) {
            response.setHeader("Content-Security-Policy", legacyPolicy());
        } else {
            String nonce = newNonce();
            request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);
            response.setHeader("Content-Security-Policy", strictPolicy(nonce));
        }
        filterChain.doFilter(request, response);
    }

    /** 静态工具页与搜索引擎验证页无法注入 nonce，按路径回退旧策略 */
    private boolean isNonceExemptPath(String uri) {
        return uri.startsWith("/tools") || uri.equals("/ByteDanceVerify.html") || uri.startsWith("/baidu_verify_");
    }

    private String newNonce() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** 严格策略：与原配置唯一区别是 script-src 以 'nonce-xxx' 取代 'unsafe-inline' */
    private String strictPolicy(String nonce) {
        return "default-src 'self'; script-src 'self' 'nonce-" + nonce + "' " + SCRIPT_HOSTS + "; " + POLICY_SUFFIX;
    }

    private String legacyPolicy() {
        return "default-src 'self'; script-src 'self' 'unsafe-inline' " + SCRIPT_HOSTS + "; " + POLICY_SUFFIX;
    }
}
