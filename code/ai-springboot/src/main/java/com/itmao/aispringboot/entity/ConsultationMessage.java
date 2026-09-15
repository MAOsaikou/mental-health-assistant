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
@TableName("consultation_message")
public class ConsultationMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("session_id")
    private Long sessionId;
    @TableField("sender_type")
    private Integer senderType;
    @TableField("message_type")
    private Integer messageType;
    private String content;
    @TableField("emotion_tag")
    private String emotionTag;
    @TableField("ai_model")
    private String aiModel;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getSenderTypeDesc() {
        if (senderType == null) {
            return "未知";
        }
        return switch (senderType) {
            case 1 -> "用户";
            case 2 -> "AI助手";
            default -> "未知";
        };
    }

    public String getMessageTypeDesc() {
        if (messageType == null) {
            return "未知";
        }
        return switch (messageType) {
            case 1 -> "文本";
            default -> "未知";
        };
    }
}
