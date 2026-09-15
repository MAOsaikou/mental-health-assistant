package com.itmao.aispringboot.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.itmao.aispringboot.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenUtil implements ApplicationContextAware {

    private static final String ISSUER = "mental-health-assistant";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        JwtTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig getJwtConfig() {
        return applicationContext.getBean(JwtConfig.class);
    }

    public static String generateAccessToken(Long userId, String username, Integer roleType, Integer tokenVersion) {
        return generateToken(userId, username, roleType, normalizeVersion(tokenVersion),
                TOKEN_TYPE_ACCESS, getJwtConfig().getExpiration());
    }

    public static String generateRefreshToken(Long userId, String username, Integer roleType,
                                              Integer tokenVersion, String tokenId) {
        return generateToken(userId, username, roleType, normalizeVersion(tokenVersion),
                TOKEN_TYPE_REFRESH, getJwtConfig().getRefreshExpiration(), tokenId);
    }

    private static String generateToken(Long userId, String username, Integer roleType,
                                        Integer tokenVersion, String tokenType, long ttlMillis) {
        return generateToken(userId, username, roleType, tokenVersion, tokenType, ttlMillis, UUID.randomUUID().toString());
    }

    private static String generateToken(Long userId, String username, Integer roleType,
                                        Integer tokenVersion, String tokenType, long ttlMillis, String tokenId) {
        try {
            JwtConfig jwtConfig = getJwtConfig();
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            Date expiration = new Date(System.currentTimeMillis() + ttlMillis);

            return JWT.create()
                    .withJWTId(tokenId)
                    .withClaim("userId", userId)
                    .withClaim("username", username)
                    .withClaim("roleType", roleType)
                    .withClaim("tokenVersion", tokenVersion)
                    .withClaim("tokenType", tokenType)
                    .withExpiresAt(expiration)
                    .withIssuedAt(new Date())
                    .withIssuer(ISSUER)
                    .sign(algorithm);
        } catch (Exception e) {
            throw new RuntimeException("生成token失败: " + e);
        }
    }

    public static String extractTokenFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String tokenHeader = request.getHeader("token");
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader;
        }

        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    public static String getCurrentToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String token = (String) request.getAttribute("jwtToken");
        if (token != null) {
            return token;
        }
        return extractTokenFromRequest(request);
    }

    public static Long getCurrentUserId() {
        Long fromRequest = getRequestAttribute("currentUserId", Long.class);
        if (fromRequest != null) {
            return fromRequest;
        }
        DecodedJWT jwt = verifyToken(getCurrentToken());
        return jwt.getClaim("userId").asLong();
    }

    public static Integer getCurrentRoleType() {
        Integer fromRequest = getRequestAttribute("currentRoleType", Integer.class);
        if (fromRequest != null) {
            return fromRequest;
        }
        DecodedJWT jwt = verifyToken(getCurrentToken());
        try {
            return jwt.getClaim("roleType").asInt();
        } catch (Exception e) {
            String roleTypeStr = jwt.getClaim("roleType").asString();
            return StringUtils.hasText(roleTypeStr) ? Integer.valueOf(roleTypeStr) : null;
        }
    }

    public static boolean isCurrentAdmin() {
        try {
            return Integer.valueOf(2).equals(getCurrentRoleType());
        } catch (Exception e) {
            return false;
        }
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    private static <T> T getRequestAttribute(String name, Class<T> type) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(name);
        if (value == null || !type.isInstance(value)) {
            return null;
        }
        return type.cast(value);
    }

    public static TokenVerificationResult validateToken(String token) {
        return validateToken(token, TOKEN_TYPE_ACCESS);
    }

    public static TokenVerificationResult validateRefreshToken(String token) {
        return validateToken(token, TOKEN_TYPE_REFRESH);
    }

    private static TokenVerificationResult validateToken(String token, String expectedType) {
        try {
            DecodedJWT jwt = verifyToken(token);
            Long userId = jwt.getClaim("userId").asLong();
            String username = jwt.getClaim("username").asString();
            String tokenType = jwt.getClaim("tokenType").asString();
            Integer tokenVersion = jwt.getClaim("tokenVersion").asInt();
            String tokenId = jwt.getId();

            Integer roleType = null;
            try {
                roleType = jwt.getClaim("roleType").asInt();
            } catch (Exception e) {
                String roleTypeStr = jwt.getClaim("roleType").asString();
                if (StringUtils.hasText(roleTypeStr)) {
                    roleType = Integer.valueOf(roleTypeStr);
                }
            }

            if (userId != null && StringUtils.hasText(username) && roleType != null
                    && tokenVersion != null && StringUtils.hasText(tokenId)
                    && expectedType.equals(tokenType)) {
                return new TokenVerificationResult(
                        userId, username, roleType, tokenVersion, tokenType, tokenId, jwt.getExpiresAt(), true);
            }
            return null;
        } catch (JWTVerificationException e) {
            return null;
        }
    }

    private static int normalizeVersion(Integer version) {
        return version == null ? 0 : version;
    }

    public static DecodedJWT verifyToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new JWTVerificationException("Token不能为空");
        }
        JwtConfig jwtConfig = getJwtConfig();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }

    @Getter
    public static class TokenVerificationResult {
        private final Long userId;
        private final String username;
        private final Integer roleType;
        private final Integer tokenVersion;
        private final String tokenType;
        private final String tokenId;
        private final Date expiresAt;
        private final boolean valid;

        public TokenVerificationResult(Long userId, String username, Integer roleType,
                                       Integer tokenVersion, String tokenType, String tokenId,
                                       Date expiresAt, boolean valid) {
            this.userId = userId;
            this.username = username;
            this.roleType = roleType;
            this.tokenVersion = tokenVersion;
            this.tokenType = tokenType;
            this.tokenId = tokenId;
            this.expiresAt = expiresAt;
            this.valid = valid;
        }
    }
}
