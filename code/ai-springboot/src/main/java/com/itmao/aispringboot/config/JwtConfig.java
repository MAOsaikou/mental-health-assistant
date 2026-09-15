package com.itmao.aispringboot.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Base64;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    private String secret;
    private long expiration;
    private long refreshExpiration;
    private String header;
    private String tokenPrefix;
    private String localSecretFile = ".data/jwt-secret.key";

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            secret = loadOrCreateLocalSecret();
            log.warn("未配置 JWT_SECRET 环境变量，已加载本机专用 JWT 密钥。生产环境仍必须通过 Secret 管理服务设置 JWT_SECRET。");
        } else if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 长度不足 32 位，存在被暴力破解风险，请更换为更长的随机字符串");
        }
    }

    private String loadOrCreateLocalSecret() {
        try {
            Path path = Path.of(localSecretFile).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                return validateLocalSecret(Files.readString(path, StandardCharsets.UTF_8).trim());
            }
            Files.createDirectories(path.getParent());
            byte[] randomBytes = new byte[64];
            new SecureRandom().nextBytes(randomBytes);
            String generated = Base64.getEncoder().encodeToString(randomBytes);
            try {
                Files.writeString(path, generated, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE_NEW,
                        java.nio.file.StandardOpenOption.WRITE);
                restrictToCurrentUser(path);
                return generated;
            } catch (FileAlreadyExistsException ignored) {
                return validateLocalSecret(Files.readString(path, StandardCharsets.UTF_8).trim());
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法创建本机 JWT 密钥文件，请设置 JWT_SECRET 环境变量", e);
        }
    }

    private String validateLocalSecret(String value) {
        if (value.length() < 32) {
            throw new IllegalStateException("本机 JWT 密钥文件内容无效，请删除后重启或设置 JWT_SECRET");
        }
        return value;
    }

    private void restrictToCurrentUser(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            return;
        } catch (UnsupportedOperationException | java.io.IOException ignored) {
            // Windows 使用 ACL，其他不支持权限视图的文件系统保持默认权限。
        }
        try {
            AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
            if (view == null) {
                return;
            }
            AclEntry ownerOnly = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                    .build();
            view.setAcl(List.of(ownerOnly));
        } catch (Exception e) {
            log.warn("无法收紧本机 JWT 密钥文件权限，请检查 {} 的 ACL", path);
        }
    }
}
