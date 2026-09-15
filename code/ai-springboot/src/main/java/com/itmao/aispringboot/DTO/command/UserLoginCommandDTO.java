package com.itmao.aispringboot.DTO.command;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data

public class UserLoginCommandDTO {

    @NotBlank(message = "用户名或者邮箱不能为空")
    @Size(max = 100, message = "用户名或者邮箱长度不能超过100个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6到50个字符之间")
    private String password;
}
