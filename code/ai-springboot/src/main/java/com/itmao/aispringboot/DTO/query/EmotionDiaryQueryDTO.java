package com.itmao.aispringboot.DTO.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EmotionDiaryQueryDTO extends PageQueryDTO {
    private Long userId;
    private String moodScreRange;
}
