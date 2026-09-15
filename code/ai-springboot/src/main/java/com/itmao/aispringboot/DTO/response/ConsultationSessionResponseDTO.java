package com.itmao.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConsultationSessionResponseDTO {
    private Long id;
    private Long userId;
    private String userNickname;
    private String sessionTitle;
    private LocalDateTime startedAt;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer messageCount;
    private Long durationMinutes;
    private String lastEmotionAnalysis;
    private Boolean contentMasked;
}
