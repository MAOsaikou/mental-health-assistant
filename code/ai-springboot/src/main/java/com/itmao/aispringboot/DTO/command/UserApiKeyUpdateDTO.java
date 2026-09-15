package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserApiKeyUpdateDTO {
    @Size(max = 255)
    private String apiKey;

    @Size(max = 255)
    private String apiBaseUrl;

    @Size(max = 100)
    private String apiModel;

    private Boolean clearKey;
}
