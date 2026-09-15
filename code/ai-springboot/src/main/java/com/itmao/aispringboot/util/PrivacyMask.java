package com.itmao.aispringboot.util;

public class PrivacyMask {

    private PrivacyMask() {
    }

    public static String mask(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String trimmed = text.trim();
        if (trimmed.length() <= 6) {
            return "已隐藏";
        }
        return trimmed.substring(0, 6) + "…（已隐藏）";
    }
}
