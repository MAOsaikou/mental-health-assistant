package com.itmao.aispringboot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrivacyMaskTest {

    @Test
    void hidesShortTextEntirely() {
        assertEquals("", PrivacyMask.mask(null));
        assertEquals("已隐藏", PrivacyMask.mask("心情差"));
    }

    @Test
    void keepsPrefixThenHidesTheRest() {
        assertEquals("今天有点累，…（已隐藏）", PrivacyMask.mask("今天有点累，不想说话"));
    }
}
