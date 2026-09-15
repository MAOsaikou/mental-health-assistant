package com.itmao.aispringboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("auth_refresh_session")
public class AuthRefreshSession {
    @TableId(type = IdType.INPUT)
    private String id;
    @TableField("user_id")
    private Long userId;
    @TableField("token_version")
    private Integer tokenVersion;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("revoked_at")
    private LocalDateTime revokedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
