package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.User;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    
    boolean checkPassword(User user, String rawPassword);
    
    User register(String username, String password, String email, String nickname);
}