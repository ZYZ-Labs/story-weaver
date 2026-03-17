package com.storyweaver.controller;

import com.storyweaver.domain.dto.LoginRequest;
import com.storyweaver.domain.entity.User;
import com.storyweaver.service.UserService;
import com.storyweaver.utils.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !userService.checkPassword(user, request.getPassword())) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 401);
            error.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(error);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("data", Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "email", user.getEmail(),
                "avatar", user.getAvatar()
            )
        ));
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody LoginRequest request) {
        User existingUser = userService.findByUsername(request.getUsername());
        if (existingUser != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("code", 400);
            error.put("message", "用户名已存在");
            return ResponseEntity.badRequest().body(error);
        }

        User user = userService.register(
            request.getUsername(),
            request.getPassword(),
            request.getUsername() + "@storyweaver.com",
            request.getUsername()
        );

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "注册成功");
        result.put("data", Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname(),
                "email", user.getEmail(),
                "avatar", user.getAvatar()
            )
        ));
        
        return ResponseEntity.ok(result);
    }
}