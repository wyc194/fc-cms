package club.freecity.cms.config;

import club.freecity.cms.security.CspNonceFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 将 {@link CspNonceFilter} 生成的 CSP nonce 以 ${cspNonce} 暴露给前台 Thymeleaf 模板。
 */
@ControllerAdvice(basePackages = "club.freecity.cms.controller.view")
public class CspNonceAdvice {

    @ModelAttribute("cspNonce")
    public String cspNonce(HttpServletRequest request) {
        return (String) request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
    }
}
