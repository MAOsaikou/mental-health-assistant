package com.itmao.aispringboot.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    @Test
    void unknownExceptionDoesNotLeakStackToClient() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Result<Void> result = handler.handleUnknown(new RuntimeException("secret-stack-detail"));
        assertEquals(ResultCode.SYSTEM_ERROR.getCode(), result.getCode());
        assertEquals("刚才出了点问题，过一会儿再试也没关系。", result.getMsg());
        assertNull(result.getData());
    }
}
