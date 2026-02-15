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
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
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
        final String tokenUsername = extractUsername(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
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

    private Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
