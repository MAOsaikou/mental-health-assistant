ALTER TABLE `user`
    ADD COLUMN `token_version` int NOT NULL DEFAULT 0 COMMENT 'JWT 失效版本';

CREATE TABLE `auth_refresh_session` (
    `id` varchar(36) NOT NULL COMMENT '刷新令牌 jti',
    `user_id` bigint NOT NULL,
    `token_version` int NOT NULL,
    `expires_at` datetime NOT NULL,
    `revoked_at` datetime NULL,
    `created_at` datetime NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_refresh_user_active` (`user_id`, `revoked_at`),
    KEY `idx_refresh_expires_at` (`expires_at`),
    CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JWT刷新会话';
