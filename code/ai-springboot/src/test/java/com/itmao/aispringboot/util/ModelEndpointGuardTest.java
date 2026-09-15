package com.itmao.aispringboot.util;

import com.itmao.aispringboot.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelEndpointGuardTest {

    private ModelEndpointGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ModelEndpointGuard("api.deepseek.com,api.openai.com", "https://api.deepseek.com");
    }

    @Test
    void allowsHttpsWhitelistHost() {
        assertEquals("https://api.deepseek.com", guard.sanitizeBaseUrl("https://api.deepseek.com/v1"));
    }

    @Test
    void fallsBackToDefaultWhenBlank() {
        assertEquals("https://api.deepseek.com", guard.sanitizeBaseUrl("  "));
    }

    @Test
    void rejectsHttpIpAndUnknownHost() {
        assertThrows(BusinessException.class, () -> guard.sanitizeBaseUrl("http://api.deepseek.com"));
        assertThrows(BusinessException.class, () -> guard.sanitizeBaseUrl("https://127.0.0.1"));
        assertThrows(BusinessException.class, () -> guard.sanitizeBaseUrl("https://evil.example.com"));
    }
}
