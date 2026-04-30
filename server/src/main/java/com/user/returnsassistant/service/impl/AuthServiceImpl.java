package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.UserAccountMapper;
import com.user.returnsassistant.pojo.LoginResponse;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserAccountMapper userAccountMapper;

    @Value("${app.auth.admin-password:123456}")
    private String adminPassword;
    @Value("${app.auth.customer-password:123456}")
    private String customerPassword;
    @Value("${app.auth.token-hours:8}")
    private Integer tokenHours;

    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    @Override
    public LoginResponse login(String username, String password) {
        if (!hasText(username) || !hasText(password)) {
            throw new BusinessException("账号和密码不能为空");
        }
        UserAccount user = userAccountMapper.getByUsername(username.trim());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号不存在或已停用");
        }
        if (!passwordMatches(user, password)) {
            throw new BusinessException("账号或密码错误");
        }
        long expiresAt = System.currentTimeMillis() + Duration.ofHours(tokenHours == null ? 8 : tokenHours).toMillis();
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new AuthSession(user, expiresAt));
        return toResponse(token, user, expiresAt);
    }

    @Override
    public LoginResponse me(String token) {
        UserAccount user = requireUser(token);
        AuthSession session = sessions.get(normalize(token));
        return toResponse(normalize(token), user, session.expiresAt());
    }

    @Override
    public UserAccount requireUser(String token) {
        String normalized = normalize(token);
        if (!hasText(normalized)) {
            throw new BusinessException("请先登录");
        }
        AuthSession session = sessions.get(normalized);
        if (session == null || session.expiresAt() < System.currentTimeMillis()) {
            sessions.remove(normalized);
            throw new BusinessException("登录已过期，请重新登录");
        }
        return session.user();
    }

    @Override
    public void logout(String token) {
        String normalized = normalize(token);
        if (hasText(normalized)) {
            sessions.remove(normalized);
        }
    }

    private boolean passwordMatches(UserAccount user, String password) {
        String expected = "ADMIN".equals(user.getRole()) ? adminPassword : customerPassword;
        return password.equals(expected);
    }

    private LoginResponse toResponse(String token, UserAccount user, long expiresAt) {
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(), expiresAt);
    }

    private String normalize(String token) {
        if (token == null) {
            return "";
        }
        String value = token.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private record AuthSession(UserAccount user, long expiresAt) {
    }
}
