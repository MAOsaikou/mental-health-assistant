package com.itmao.aispringboot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlContentSanitizerTest {

    @Test
    void removesExecutableMarkupAndDangerousAttributes() {
        String dirty = "<p onclick=\"alert(1)\">你好<script>alert(2)</script>"
                + "<a href=\"javascript:alert(3)\">链接</a></p>";

        String clean = HtmlContentSanitizer.sanitize(dirty);

        assertFalse(clean.contains("script"));
        assertFalse(clean.contains("onclick"));
        assertFalse(clean.contains("javascript:"));
        assertTrue(clean.contains("<p>你好"));
    }

    @Test
    void keepsExpectedRichTextAndHardensLinks() {
        String clean = HtmlContentSanitizer.sanitize(
                "<h2>标题</h2><p><strong>正文</strong>"
                        + "<a href=\"https://example.com\">资料</a></p>");

        assertTrue(clean.contains("<h2>标题</h2>"));
        assertTrue(clean.contains("<strong>正文</strong>"));
        assertTrue(clean.contains("href=\"https://example.com\""));
        assertTrue(clean.contains("rel=\"nofollow noopener noreferrer\""));
    }

    @Test
    void preservesNullAndBlankValues() {
        assertEquals(null, HtmlContentSanitizer.sanitize(null));
        assertEquals("   ", HtmlContentSanitizer.sanitize("   "));
    }
}
