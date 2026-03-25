package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.UserCreateDTO;
import com.storyweaver.domain.dto.UserUpdateDTO;
import com.storyweaver.domain.entity.User;
import com.storyweaver.domain.vo.LoginFailureStateVO;
import com.storyweaver.repository.UserMapper;
import com.storyweaver.service.SystemConfigService;
import com.storyweaver.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";

    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;

    public UserServiceImpl(PasswordEncoder passwordEncoder, SystemConfigService systemConfigService) {
        this.passwordEncoder = passwordEncoder;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public User findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username.trim());
        return getOne(queryWrapper, false);
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return user != null && StringUtils.hasText(rawPassword) && passwordEncoder.matches(rawPassword, user.getPassword());
    }

    @Override
    @Transactional
    public User register(String username, String password, String email, String nickname) {
        if (!isRegistrationEnabled()) {
            throw new IllegalStateException("当前环境已关闭公开注册，请联系管理员创建账号");
        }

        UserCreateDTO request = new UserCreateDTO();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        request.setNickname(nickname);
        request.setRoleCode(ROLE_USER);
        request.setStatus(1);
        return createUser(request);
    }

    @Override
    @Transactional
    public User createUser(UserCreateDTO request) {
        String username = normalizeUsername(request.getUsername());
        if (findByUsername(username) != null) {
            throw new IllegalStateException("用户名已存在");
        }

        String roleCode = normalizeRoleCode(request.getRoleCode());
        validatePasswordStrength(request.getPassword());

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setNickname(normalizeNickname(request.getNickname(), username));
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setRoleCode(roleCode);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        save(user);
        return getById(user.getId());
    }

    @Override
    @Transactional
    public User updateManagedUser(Long userId, UserUpdateDTO request) {
        User existing = getById(userId);
        if (existing == null) {
            throw new IllegalStateException("账号不存在");
        }

        String nextRoleCode = normalizeRoleCode(request.getRoleCode());
        Integer nextStatus = normalizeStatus(request.getStatus());
        ensureLastAdminRemains(existing, nextRoleCode, nextStatus);

        existing.setNickname(normalizeNickname(request.getNickname(), existing.getUsername()));
        existing.setEmail(normalizeNullable(request.getEmail()));
        existing.setRoleCode(nextRoleCode);
        existing.setStatus(nextStatus);
        updateById(existing);
        return getById(userId);
    }

    @Override
    @Transactional
    public User resetPassword(Long userId, String newPassword) {
        User existing = getById(userId);
        if (existing == null) {
            throw new IllegalStateException("账号不存在");
        }

        validatePasswordStrength(newPassword);
        LocalDateTime now = LocalDateTime.now();
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(newPassword.trim()))
                .set(User::getFailedLoginAttempts, 0)
                .set(User::getLockedUntil, null)
                .set(User::getPasswordChangedAt, now)
                .update();
        return getById(userId);
    }

    @Override
    @Transactional
    public User unlockUser(Long userId) {
        User existing = getById(userId);
        if (existing == null) {
            throw new IllegalStateException("账号不存在");
        }

        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getFailedLoginAttempts, 0)
                .set(User::getLockedUntil, null)
                .update();
        return getById(userId);
    }

    @Override
    public List<User> listUsersForAdmin() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("id");
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public LoginFailureStateVO recordFailedLogin(User user) {
        User existing = getById(user.getId());
        if (existing == null) {
            throw new IllegalStateException("账号不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (existing.getLockedUntil() != null && existing.getLockedUntil().isBefore(now)) {
            existing.setLockedUntil(null);
            existing.setFailedLoginAttempts(0);
        }

        int failedAttempts = (existing.getFailedLoginAttempts() == null ? 0 : existing.getFailedLoginAttempts()) + 1;
        int maxAttempts = getMaxFailedAttempts();
        boolean locked = failedAttempts >= maxAttempts;

        LocalDateTime lockedUntil = locked ? now.plusMinutes(getLockMinutes()) : null;
        lambdaUpdate()
                .eq(User::getId, existing.getId())
                .set(User::getFailedLoginAttempts, failedAttempts)
                .set(User::getLockedUntil, lockedUntil)
                .update();

        return new LoginFailureStateVO(
                failedAttempts,
                Math.max(maxAttempts - failedAttempts, 0),
                locked,
                lockedUntil
        );
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(User user) {
        User existing = getById(user.getId());
        if (existing == null) {
            return;
        }

        lambdaUpdate()
                .eq(User::getId, existing.getId())
                .set(User::getFailedLoginAttempts, 0)
                .set(User::getLockedUntil, null)
                .set(User::getLastLoginAt, LocalDateTime.now())
                .update();
    }

    @Override
    public boolean isLocked(User user) {
        return user != null && user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && ROLE_ADMIN.equalsIgnoreCase(user.getRoleCode());
    }

    @Override
    public boolean isRegistrationEnabled() {
        return getBooleanConfig("registration_enabled", false);
    }

    @Override
    public int getMaxFailedAttempts() {
        return getIntConfig("auth.max_failed_attempts", 5);
    }

    @Override
    public int getLockMinutes() {
        return getIntConfig("auth.lock_minutes", 30);
    }

    private void validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("密码不能为空");
        }

        String trimmed = password.trim();
        if (trimmed.length() < 8) {
            throw new IllegalStateException("密码长度不能少于 8 位");
        }
        if (trimmed.length() > 64) {
            throw new IllegalStateException("密码长度不能超过 64 位");
        }

        boolean hasLetter = trimmed.chars().anyMatch(Character::isLetter);
        boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new IllegalStateException("密码至少需要包含字母和数字");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = normalizeNullable(roleCode);
        if (!StringUtils.hasText(normalized)) {
            return ROLE_USER;
        }

        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!ROLE_ADMIN.equals(normalized) && !ROLE_USER.equals(normalized)) {
            throw new IllegalStateException("不支持的账号角色");
        }
        return normalized;
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new IllegalStateException("账号状态无效");
        }
        return status;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException("用户名不能为空");
        }
        String normalized = username.trim();
        if (normalized.length() > 50) {
            throw new IllegalStateException("用户名长度不能超过 50 个字符");
        }
        return normalized;
    }

    private String normalizeNickname(String nickname, String fallback) {
        if (!StringUtils.hasText(nickname)) {
            return fallback;
        }
        String normalized = nickname.trim();
        if (normalized.length() > 50) {
            throw new IllegalStateException("昵称长度不能超过 50 个字符");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int getIntConfig(String configKey, int defaultValue) {
        String value = systemConfigService.getConfigValue(configKey);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private boolean getBooleanConfig(String configKey, boolean defaultValue) {
        String value = systemConfigService.getConfigValue(configKey);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim());
    }

    private void ensureLastAdminRemains(User existing, String nextRoleCode, Integer nextStatus) {
        boolean isEnabledAdmin = isAdmin(existing) && Integer.valueOf(1).equals(existing.getStatus());
        boolean staysEnabledAdmin = ROLE_ADMIN.equalsIgnoreCase(nextRoleCode) && Integer.valueOf(1).equals(nextStatus);
        if (!isEnabledAdmin || staysEnabledAdmin) {
            return;
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role_code", ROLE_ADMIN)
                .eq("status", 1)
                .ne("id", existing.getId());

        if (count(queryWrapper) == 0) {
            throw new IllegalStateException("至少需要保留一个启用中的管理员账号");
        }
    }
}
