package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserResetPasswordDTO {
    @NotBlank(message = "新密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64 个字符")
    private String newPassword;
}
