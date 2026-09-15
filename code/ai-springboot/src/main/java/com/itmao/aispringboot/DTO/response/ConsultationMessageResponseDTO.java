package com.itmao.aispringboot.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationMessageResponseDTO {
    private Long id;
    private Long sessionId;
    private Integer senderType;
    private String senderTypeDesc;
    private Integer messageType;
    private String messageTypeDesc;
    private String content;
    private String emotionTag;
    private String aiModel;
    private LocalDateTime createdAt;
    private Integer contentLength;
    private Boolean contentMasked;

    public void calculateContentLength() {
        this.contentLength = content != null ? content.length() : 0;
    }
}
