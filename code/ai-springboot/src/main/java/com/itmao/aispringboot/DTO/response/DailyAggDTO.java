package com.itmao.aispringboot.DTO.response;

import lombok.Data;

@Data
public class DailyAggDTO {
    private String date;
    private Long recordCount;
    private Double avgMoodScore;
    private Long sessionCount;
    private Long userCount;
    private Long newUsers;
    private Long diaryUsers;
    private Long consultationUsers;
}
