package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.UserCreateDTO;
import com.storyweaver.domain.dto.UserUpdateDTO;
import com.storyweaver.domain.entity.User;
import com.storyweaver.domain.vo.LoginFailureStateVO;

import java.util.List;

public interface UserService extends IService<User> {
    User findByUsername(String username);

    boolean checkPassword(User user, String rawPassword);

    User register(String username, String password, String email, String nickname);

    User createUser(UserCreateDTO request);

    User updateManagedUser(Long userId, UserUpdateDTO request);

    User resetPassword(Long userId, String newPassword);

    User unlockUser(Long userId);

    List<User> listUsersForAdmin();

    LoginFailureStateVO recordFailedLogin(User user);

    void recordSuccessfulLogin(User user);

    boolean isLocked(User user);

    boolean isAdmin(User user);

    boolean isRegistrationEnabled();

    int getMaxFailedAttempts();

    int getLockMinutes();
}
