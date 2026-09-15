package com.itmao.aispringboot.DTO.response;

import lombok.Data;

@Data
public class SessionMessageCountDTO {
    private Long sessionId;
    private Integer messageCount;
}
