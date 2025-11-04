package com.everypoll.authService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {
    @NotBlank(message = "아이디를 입력해 주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자리 이상 20자리 미만이여야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인이 입력되지 않았습니다.")
    private String confirmPassword;

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")   
    private String email;
}
