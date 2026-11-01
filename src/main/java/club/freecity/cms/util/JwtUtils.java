package club.freecity.cms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import club.freecity.cms.common.SecurityConstants;

import javax.crypto.SecretKey;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey key;

    private static final long ACCESS_EXPIRATION = 7200000; // 2 hours
    private static final long REFRESH_EXPIRATION = 604800000; // 7 days

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置：请通过环境变量 APP_JWT_SECRET 提供一个 Base64 编码且长度不少于 32 字节的密钥");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        // HMAC-SHA256 要求至少 256 位（32 字节）密钥，否则签名强度不足
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥强度不足：解码后长度为 " + keyBytes.length + " 字节，要求至少 32 字节（256 位）");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String username, String role, Long tenantId, String tenantCode, LocalDateTime passwordUpdateTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, userId);
        claims.put(SecurityConstants.CLAIM_ROLE, role);
        claims.put(SecurityConstants.CLAIM_TENANT_ID, tenantId);
        claims.put(SecurityConstants.CLAIM_TENANT_CODE, tenantCode);
        claims.put(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_ACCESS);
        if (passwordUpdateTime != null) {
            claims.put(SecurityConstants.CLAIM_PASSWORD_UPDATE_TIME, passwordUpdateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return createToken(claims, username, ACCESS_EXPIRATION);
    }

    public String generateRefreshToken(Long userId, String username, String role, Long tenantId, String tenantCode, LocalDateTime passwordUpdateTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(SecurityConstants.CLAIM_USER_ID, userId);
        claims.put(SecurityConstants.CLAIM_ROLE, role);
        claims.put(SecurityConstants.CLAIM_TENANT_ID, tenantId);
        claims.put(SecurityConstants.CLAIM_TENANT_CODE, tenantCode);
        claims.put(SecurityConstants.CLAIM_TYPE, SecurityConstants.TOKEN_TYPE_REFRESH);
        if (passwordUpdateTime != null) {
            claims.put(SecurityConstants.CLAIM_PASSWORD_UPDATE_TIME, passwordUpdateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        return createToken(claims, username, REFRESH_EXPIRATION);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Boolean validateToken(String token, String username) {
        // 只解析一次 Claims，避免链路上重复解析 JWT
        final Claims claims = extractAllClaims(token);
        return (claims.getSubject().equals(username) && claims.getExpiration().before(new Date()));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        final Claims claims = extractAllClaims(token);
        Object userId = claims.get(SecurityConstants.CLAIM_USER_ID);
        if (userId == null) {
            return null;
        }
        return userId instanceof Number number ? number.longValue() : Long.valueOf(userId.toString());
    }

    public Long extractIssuedAt(String token) {
        final Claims claims = extractAllClaims(token);
        Date issuedAt = claims.getIssuedAt();
        return issuedAt == null ? null : issuedAt.getTime();
    }

    public Long extractTenantId(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get(SecurityConstants.CLAIM_TENANT_ID, Long.class);
    }

    public String extractTenantCode(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get(SecurityConstants.CLAIM_TENANT_CODE, String.class);
    }

    public String extractRole(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get(SecurityConstants.CLAIM_ROLE, String.class);
    }

    public String extractType(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get(SecurityConstants.CLAIM_TYPE, String.class);
    }

    public Long extractPasswordUpdateTime(String token) {
        final Claims claims = extractAllClaims(token);
        return claims.get(SecurityConstants.CLAIM_PASSWORD_UPDATE_TIME, Long.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}
