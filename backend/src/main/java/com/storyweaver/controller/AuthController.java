package com.storyweaver.controller;

import com.storyweaver.domain.dto.LoginRequest;
import com.storyweaver.domain.entity.User;
import com.storyweaver.domain.vo.AuthPublicConfigVO;
import com.storyweaver.domain.vo.LoginFailureStateVO;
import com.storyweaver.service.UserService;
import com.storyweaver.utils.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final DateTimeFormatter LOCK_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/public-config")
    public ResponseEntity<Map<String, Object>> publicConfig() {
        AuthPublicConfigVO config = new AuthPublicConfigVO(
                userService.isRegistrationEnabled(),
                userService.getMaxFailedAttempts(),
                userService.getLockMinutes()
        );
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", config));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null) {
            return unauthorized("用户名或密码错误");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            return error(HttpStatus.FORBIDDEN, 403, "账号已被禁用，请联系管理员");
        }

        if (userService.isLocked(user)) {
            String lockedUntil = user.getLockedUntil() == null
                    ? ""
                    : user.getLockedUntil().format(LOCK_TIME_FORMATTER);
            return error(HttpStatus.LOCKED, 423, "账号已锁定，请于 " + lockedUntil + " 后重试或联系管理员");
        }

        if (!userService.checkPassword(user, request.getPassword())) {
            LoginFailureStateVO failureState = userService.recordFailedLogin(user);
            if (failureState.locked()) {
                String lockedUntil = failureState.lockedUntil() == null
                        ? ""
                        : failureState.lockedUntil().format(LOCK_TIME_FORMATTER);
                return error(HttpStatus.LOCKED, 423, "密码连续错误过多，账号已锁定至 " + lockedUntil);
            }
            return unauthorized("用户名或密码错误，还可尝试 " + failureState.remainingAttempts() + " 次");
        }

        userService.recordSuccessfulLogin(user);
        User latestUser = userService.getById(user.getId());
        String token = jwtUtil.generateToken(latestUser.getUsername(), latestUser.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("data", buildAuthPayload(token, latestUser));

        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody LoginRequest request) {
        if (!userService.isRegistrationEnabled()) {
            return error(HttpStatus.FORBIDDEN, 403, "当前环境已关闭公开注册，请联系管理员创建账号");
        }

        User existingUser = userService.findByUsername(request.getUsername());
        if (existingUser != null) {
            return error(HttpStatus.BAD_REQUEST, 400, "用户名已存在");
        }

        User user = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getUsername() + "@storyweaver.com",
                request.getUsername()
        );

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功");
        result.put("data", buildAuthPayload(token, user));

        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, 401, message);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        return ResponseEntity.status(status).body(error);
    }

    private Map<String, Object> buildAuthPayload(String token, User user) {
        Map<String, Object> userPayload = new LinkedHashMap<>();
        userPayload.put("id", user.getId());
        userPayload.put("username", user.getUsername());
        userPayload.put("nickname", user.getNickname());
        userPayload.put("email", user.getEmail());
        userPayload.put("avatar", user.getAvatar());
        userPayload.put("roleCode", user.getRoleCode());
        userPayload.put("status", user.getStatus());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", userPayload);
        return data;
    }
}
