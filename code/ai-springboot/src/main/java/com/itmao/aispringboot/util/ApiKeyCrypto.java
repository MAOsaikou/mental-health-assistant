package com.itmao.aispringboot.util;

import com.itmao.aispringboot.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 用户在网页里填写的 API Key 入库前加密（AES-256-GCM）。
 * 密文格式：enc:v1:base64(iv + ciphertext)，无该前缀的值视为历史明文，原样返回。
 */
@Component
public class ApiKeyCrypto {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyCrypto.class);
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final Path LOCAL_SECRET_FILE = Path.of(".data", "api-key-enc.secret");

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.api-key-enc-secret:}")
    private String encSecret;

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() throws Exception {
        if (encSecret == null || encSecret.isBlank()) {
            encSecret = loadOrCreateLocalSecret();
        }
        byte[] key = MessageDigest.getInstance("SHA-256").digest(encSecret.getBytes(StandardCharsets.UTF_8));
        keySpec = new SecretKeySpec(key, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank() || keySpec == null) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException("API Key 加密失败，请稍后重试");
        }
    }

    public String decrypt(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return stored;
        }
        if (keySpec == null) {
            throw new BusinessException("服务器加密密钥不可用，无法读取 API Key，请联系管理员");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("API Key 解密失败，请重新设置你的 API Key");
        }
    }

    private String loadOrCreateLocalSecret() throws Exception {
        if (Files.exists(LOCAL_SECRET_FILE)) {
            String existing = Files.readString(LOCAL_SECRET_FILE, StandardCharsets.UTF_8).trim();
            if (!existing.isBlank()) {
                return existing;
            }
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String generated = Base64.getEncoder().encodeToString(randomBytes);
        Files.createDirectories(LOCAL_SECRET_FILE.getParent());
        Files.writeString(LOCAL_SECRET_FILE, generated, StandardCharsets.UTF_8);
        log.info("未配置 API_KEY_ENC_SECRET，已生成本地加密密钥文件 {}（用户填写的 Key 会加密入库，无需设置环境变量）", LOCAL_SECRET_FILE.toAbsolutePath());
        return generated;
    }
}
