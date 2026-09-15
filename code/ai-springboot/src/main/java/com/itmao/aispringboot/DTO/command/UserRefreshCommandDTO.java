package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRefreshCommandDTO {
    @Size(max = 4096, message = "刷新令牌格式不正确")
    private String refreshToken;
}
