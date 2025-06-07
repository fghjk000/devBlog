package com.cos.devblog.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDto {

    @Size(min = 3, max = 20)
    @NotEmpty(message = "사용자 이름은 필수 항목입니다.")
    private String username;

    @NotEmpty(message = "비밀번호는 필수항목입니다.")
    private String password;

    @Email
    @NotEmpty(message = "이메일은 필수항목입니다.")
    private String email;

}
