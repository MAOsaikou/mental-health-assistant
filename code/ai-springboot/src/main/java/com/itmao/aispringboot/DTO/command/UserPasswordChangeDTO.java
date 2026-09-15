package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordChangeDTO {
    @NotBlank(message = "请先填写现在的密码")
    private String oldPassword;

    @NotBlank(message = "请填写新密码")
    @Size(min = 6, max = 50, message = "新密码至少6位")
    private String newPassword;

    @NotBlank(message = "请再确认一次新密码")
    private String confirmPassword;
}
