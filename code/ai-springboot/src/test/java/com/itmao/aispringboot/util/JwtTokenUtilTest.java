package com.itmao.aispringboot.util;

import com.itmao.aispringboot.config.JwtConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtTokenUtilTest {
    private GenericApplicationContext context;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-that-is-longer-than-thirty-two-characters");
        config.setExpiration(1_800_000L);
        config.setRefreshExpiration(604_800_000L);
        context = new GenericApplicationContext();
        context.registerBean(JwtConfig.class, () -> config);
        context.refresh();
        new JwtTokenUtil().setApplicationContext(context);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void accessAndRefreshTokensCannotBeUsedInterchangeably() {
        String access = JwtTokenUtil.generateAccessToken(7L, "alice", 1, 3);
        String refresh = JwtTokenUtil.generateRefreshToken(7L, "alice", 1, 3, "refresh-id");

        JwtTokenUtil.TokenVerificationResult accessResult = JwtTokenUtil.validateToken(access);
        JwtTokenUtil.TokenVerificationResult refreshResult = JwtTokenUtil.validateRefreshToken(refresh);

        assertNotNull(accessResult);
        assertEquals(3, accessResult.getTokenVersion());
        assertEquals(JwtTokenUtil.TOKEN_TYPE_ACCESS, accessResult.getTokenType());
        assertNotNull(refreshResult);
        assertEquals("refresh-id", refreshResult.getTokenId());
        assertNull(JwtTokenUtil.validateToken(refresh));
        assertNull(JwtTokenUtil.validateRefreshToken(access));
    }
}
