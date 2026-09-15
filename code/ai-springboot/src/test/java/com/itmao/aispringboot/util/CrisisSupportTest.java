package com.itmao.aispringboot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrisisSupportTest {

    @Test
    void detectsCrisisKeywords() {
        assertTrue(CrisisSupport.isCrisis("我真的不想活了"));
        assertTrue(CrisisSupport.isCrisis("有点想自杀"));
        assertTrue(CrisisSupport.isCrisis("准备自残"));
        assertTrue(CrisisSupport.isCrisis("我只想永远睡着，再也不醒"));
        assertTrue(CrisisSupport.isCrisis("我真的撑 不 下 去 了"));
    }

    @Test
    void ignoresEverydayText() {
        assertFalse(CrisisSupport.isCrisis(null));
        assertFalse(CrisisSupport.isCrisis("   "));
        assertFalse(CrisisSupport.isCrisis("今天天气不错，想出去走走"));
    }

    @Test
    void fixedReplyContainsCurrentNationalHotlineAndEmergencySteps() {
        assertTrue(CrisisSupport.FIXED_REPLY.contains("12356"));
        assertTrue(CrisisSupport.FIXED_REPLY.contains("120 / 110"));
        assertTrue(CrisisSupport.FIXED_REPLY.contains("信任的人"));
    }
}
