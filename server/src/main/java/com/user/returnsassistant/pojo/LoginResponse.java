package com.user.returnsassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String displayName;
    private String role;
    private Long expiresAt;
}
