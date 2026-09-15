package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileUpdateDTO {
    @Size(max = 50, message = "昵称不要超过50个字")
    private String nickname;

    @Email(message = "邮箱格式不太对")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不太对")
    private String phone;

    private Integer gender;

    private LocalDate birthday;

    @Size(max = 500)
    private String avatar;
}
