package com.storyweaver.controller;

import com.storyweaver.domain.dto.UserCreateDTO;
import com.storyweaver.domain.dto.UserResetPasswordDTO;
import com.storyweaver.domain.dto.UserUpdateDTO;
import com.storyweaver.domain.entity.User;
import com.storyweaver.domain.vo.ManagedUserVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        List<ManagedUserVO> users = userService.listUsersForAdmin().stream().map(this::toManagedUserVO).toList();
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", users));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody UserCreateDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        User user = userService.createUser(request);
        return ResponseEntity.ok(Map.of("code", 200, "message", "账号创建成功", "data", toManagedUserVO(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        User user = userService.updateManagedUser(id, request);
        return ResponseEntity.ok(Map.of("code", 200, "message", "账号更新成功", "data", toManagedUserVO(user)));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserResetPasswordDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        User user = userService.resetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(Map.of("code", 200, "message", "密码已重置", "data", toManagedUserVO(user)));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Map<String, Object>> unlockUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        User user = userService.unlockUser(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "账号已解锁", "data", toManagedUserVO(user)));
    }

    private void requireAdmin(String authorizationHeader, Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("未认证或 token 无效");
        }
        SecurityUtils.requireAdmin(authentication);
    }

    private ManagedUserVO toManagedUserVO(User user) {
        return new ManagedUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                user.getStatus(),
                user.getRoleCode(),
                user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts(),
                userService.isLocked(user),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getPasswordChangedAt(),
                user.getCreateTime(),
                user.getUpdateTime()
        );
    }
}
