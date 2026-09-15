package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleStatusUpdateDTO {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
