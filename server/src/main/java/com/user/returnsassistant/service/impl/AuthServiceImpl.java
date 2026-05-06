package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.UserAccountMapper;
import com.user.returnsassistant.pojo.LoginResponse;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.utils.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,30}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    @Autowired
    private UserAccountMapper userAccountMapper;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.auth.admin-password:123456}")
    private String adminPassword;
    @Value("${app.auth.customer-password:123456}")
    private String customerPassword;
    @Value("${app.auth.token-hours:8}")
    private Integer tokenHours;

    private final Map<String, Long> revokedJwtIds = new ConcurrentHashMap<>();

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
        JwtTokenProvider.JwtToken token = createJwt(user);
        return toResponse(token.token(), user, token.expiresAt());
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
        JwtTokenProvider.JwtToken token = createJwt(user);
        return toResponse(token.token(), user, token.expiresAt());
    }

    @Override
    public LoginResponse me(String token) {
        String normalized = normalize(token);
        if (!hasText(normalized)) {
            throw new BusinessException("请先登录");
        }
        JwtTokenProvider.JwtUser jwtUser = parseRequired(normalized);
        UserAccount user = requireActiveUser(jwtUser.userId());
        return toResponse(normalized, user, jwtUser.expiresAt());
    }

    @Override
    public UserAccount requireUser(String token) {
        String normalized = normalize(token);
        if (!hasText(normalized)) {
            throw new BusinessException("请先登录");
        }
        JwtTokenProvider.JwtUser jwtUser = parseRequired(normalized);
        return requireActiveUser(jwtUser.userId());
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
        if (!hasText(normalized)) {
            return;
        }
        try {
            JwtTokenProvider.JwtUser jwtUser = jwtTokenProvider.parseToken(normalized);
            if (hasText(jwtUser.jwtId())) {
                revokedJwtIds.put(jwtUser.jwtId(), jwtUser.expiresAt());
            }
        } catch (IllegalArgumentException ignored) {
            // Expired or malformed tokens are already unusable.
        }
    }

    private JwtTokenProvider.JwtToken createJwt(UserAccount user) {
        cleanupRevokedJwtIds();
        return jwtTokenProvider.createToken(user, Duration.ofHours(tokenHours == null ? 8 : tokenHours));
    }

    private JwtTokenProvider.JwtUser parseRequired(String token) {
        cleanupRevokedJwtIds();
        JwtTokenProvider.JwtUser jwtUser;
        try {
            jwtUser = jwtTokenProvider.parseToken(token);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("登录已过期，请重新登录");
        }
        Long revokedUntil = revokedJwtIds.get(jwtUser.jwtId());
        if (revokedUntil != null && revokedUntil > System.currentTimeMillis()) {
            throw new BusinessException("登录已退出，请重新登录");
        }
        return jwtUser;
    }

    private UserAccount requireActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        UserAccount user = userAccountMapper.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号不存在或已停用");
        }
        return user;
    }

    private void cleanupRevokedJwtIds() {
        long now = System.currentTimeMillis();
        revokedJwtIds.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() <= now);
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
}
