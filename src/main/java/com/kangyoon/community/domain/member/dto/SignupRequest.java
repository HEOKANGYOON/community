package com.kangyoon.community.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest (
        @NotBlank @Size(max = 100) @Email String email,
        @NotBlank @Size(min = 8, max = 80) String password,
        //닉네임은 12글자 제한 및 한글, 영어(대소문자), 숫자만 가능
        @NotBlank @Size(min = 2, max = 12) @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$") String nickname
) {}
