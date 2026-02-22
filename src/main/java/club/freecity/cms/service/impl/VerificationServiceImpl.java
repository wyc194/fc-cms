package club.freecity.cms.service.impl;

import club.freecity.cms.config.CacheConfig;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.service.EmailService;
import club.freecity.cms.service.VerificationService;
import club.freecity.cms.support.ratelimit.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationServiceImpl implements VerificationService {

    private final EmailService emailService;
    private final CacheManager cacheManager;
    private final RateLimiter rateLimiter;

    @Override
    public void sendVerificationCode(String email, String ip) {
        // 1. 限流检查: 同一 IP 和 邮箱 每 60 秒只能发送一次
        String limitKey = "email_code:" + ip + ":" + email;
        if (!rateLimiter.isAllowed(limitKey, 1, 60)) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }

        // 2. 生成 6 位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 3. 存入缓存
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_VERIFICATION_CODE);
        if (cache != null) {
            cache.put(email, code);
        }

        // 4. 发送邮件
        String subject = "【FreeCity】评论邮箱验证码";
        String content = String.format(
                "<div style='padding: 20px; border: 1px solid #eee; border-radius: 5px; font-family: sans-serif;'>" +
                "<h3>您好，</h3>" +
                "<p>您正在 FreeCity 博客提交评论，您的验证码为：</p>" +
                "<p style='font-size: 24px; font-weight: bold; color: #409EFF; letter-spacing: 5px; margin: 20px 0;'>%s</p>" +
                "<p>验证码 5 分钟内有效，请勿泄露给他人。若非本人操作，请忽略此邮件。</p>" +
                "<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>" +
                "<p style='color: #999; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>" +
                "</div>", code);
        
        emailService.sendHtmlMail(email, subject, content);
        log.info("验证码已发送至邮箱: {}, code: {}", email, code);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_VERIFICATION_CODE);
        if (cache == null) {
            return false;
        }
        
        String cachedCode = cache.get(email, String.class);
        if (cachedCode != null && cachedCode.equals(code)) {
            // 校验成功后立即清除缓存
            cache.evict(email);
            return true;
        }
        return false;
    }
}
