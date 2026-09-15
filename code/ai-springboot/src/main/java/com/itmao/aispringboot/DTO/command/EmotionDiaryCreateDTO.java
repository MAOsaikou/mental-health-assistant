package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmotionDiaryCreateDTO {
    private LocalDate diaryDate;
    @NotNull(message = "情绪评分不能为空")
    @Min(value = 1, message = "情绪评分最小为1")
    @Max(value = 10, message = "情绪评分最大为10")
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    @Min(1) @Max(5)
    private Integer sleepQuality;
    @Min(1) @Max(5)
    private Integer stressLevel;
}
