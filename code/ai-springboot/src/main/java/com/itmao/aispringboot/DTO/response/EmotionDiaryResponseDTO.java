package com.itmao.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EmotionDiaryResponseDTO {
    private Long id;
    private LocalDate diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    private String aiEmotionAnalysis;
    private LocalDateTime createdAt;
    private Boolean contentMasked;
}
