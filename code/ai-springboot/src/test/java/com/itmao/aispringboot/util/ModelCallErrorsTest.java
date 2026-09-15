package com.itmao.aispringboot.util;

import com.itmao.aispringboot.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelCallErrorsTest {

    @Test
    void platformAuthFailureDoesNotAskUserToCheckOwnKey() {
        Throwable error = new CompletionException(
                new RuntimeException("401: Authentication Fails, Your api key: ****69b3 is invalid"));
        assertTrue(ModelCallErrors.isAuthFailure(error));
        assertEquals(ModelCallErrors.PLATFORM_KEY_UNAVAILABLE, ModelCallErrors.userMessage(false, error));
        assertEquals(ModelCallErrors.USER_KEY_INVALID, ModelCallErrors.userMessage(true, error));
    }

    @Test
    void unrelated401IsNotTreatedAsMissingBalance() {
        assertFalse(ModelCallErrors.isAuthFailure(new RuntimeException("HTTP 401 from login")));
        assertEquals(ModelCallErrors.SIGNAL_BAD, ModelCallErrors.userMessage(false, new RuntimeException("timeout")));
    }

    @Test
    void streamPrefersBusinessExceptionMessage() {
        BusinessException business = new BusinessException(ModelCallErrors.PLATFORM_KEY_UNAVAILABLE);
        assertEquals(
                ModelCallErrors.PLATFORM_KEY_UNAVAILABLE,
                ModelCallErrors.streamErrorMessage(new CompletionException(business)));
    }
}
