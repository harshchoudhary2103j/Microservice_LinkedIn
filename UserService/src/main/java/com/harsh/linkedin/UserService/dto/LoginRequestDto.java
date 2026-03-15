package com.harsh.linkedin.UserService.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String email;
    private String password;
}
