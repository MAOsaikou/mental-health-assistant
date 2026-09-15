package com.itmao.aispringboot.util;

import com.itmao.aispringboot.exception.BusinessException;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestRateLimiterTest {

    @Test
    void blocksWhenWindowIsFull() {
        RequestRateLimiter limiter = new RequestRateLimiter();
        Duration window = Duration.ofMinutes(1);
        assertDoesNotThrow(() -> limiter.assertAllowed("login:ip", 2, window));
        assertDoesNotThrow(() -> limiter.assertAllowed("login:ip", 2, window));
        BusinessException error = assertThrows(BusinessException.class,
                () -> limiter.assertAllowed("login:ip", 2, window));
        assert error.getMessage().contains("试得有点勤");
    }

    @Test
    void ignoresForwardedHeadersUnlessTrustedProxyIsConfigured() {
        RequestRateLimiter limiter = new RequestRateLimiter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.99");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertEquals("127.0.0.1", limiter.clientIp(request));
    }
}
