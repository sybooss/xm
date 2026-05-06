package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.UserAccountMapper;
import com.user.returnsassistant.pojo.LoginResponse;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,30}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

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
    public LoginResponse register(String username, String password, String confirmPassword, String displayName, String phone) {
        String cleanUsername = normalizeText(username);
        String cleanDisplayName = normalizeText(displayName);
        String cleanPhone = normalizeText(phone);
        validateRegisterInput(cleanUsername, password, confirmPassword, cleanDisplayName, cleanPhone);
        if (userAccountMapper.getByUsername(cleanUsername) != null) {
            throw new BusinessException("账号已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(cleanUsername);
        user.setDisplayName(hasText(cleanDisplayName) ? cleanDisplayName : cleanUsername);
        user.setRole("CUSTOMER");
        user.setPhone(hasText(cleanPhone) ? cleanPhone : null);
        user.setPasswordHash(hashPassword(password));
        user.setStatus(1);
        userAccountMapper.insert(user);
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
    public UserAccount requireAdmin(String token) {
        UserAccount user = requireUser(token);
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException("权限不足，仅管理员可执行该操作");
        }
        return user;
    }

    @Override
    public UserAccount requireCustomer(String token) {
        UserAccount user = requireUser(token);
        if (!"CUSTOMER".equals(user.getRole())) {
            throw new BusinessException("权限不足，仅客户可使用顾客端接口");
        }
        return user;
    }

    @Override
    public void logout(String token) {
        String normalized = normalize(token);
        if (hasText(normalized)) {
            sessions.remove(normalized);
        }
    }

    private boolean passwordMatches(UserAccount user, String password) {
        if (hasText(user.getPasswordHash())) {
            return hashPassword(password).equals(user.getPasswordHash());
        }
        String expected = "ADMIN".equals(user.getRole()) ? adminPassword : customerPassword;
        return password.equals(expected);
    }

    private void validateRegisterInput(String username, String password, String confirmPassword, String displayName, String phone) {
        if (!hasText(username) || !hasText(password) || !hasText(confirmPassword)) {
            throw new BusinessException("账号和密码不能为空");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("账号需为4-30位字母、数字或下划线");
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new BusinessException("密码需为6-32位");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (hasText(displayName) && displayName.length() > 80) {
            throw new BusinessException("昵称不能超过80个字符");
        }
        if (hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(("returns-assistant:" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
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
