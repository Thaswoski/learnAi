package com.griya.learn.service;

import com.griya.learn.dto.ChangePasswordRequest;
import com.griya.learn.dto.LoginRequest;
import com.griya.learn.dto.LoginResponse;
import com.griya.learn.dto.RegisterRequest;
import com.griya.learn.dto.UpdateProfileRequest;
import com.griya.learn.entity.User;
import com.griya.learn.exception.BusinessException;
import com.griya.learn.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        User exist = userMapper.selectByEmail(req.getEmail());
        if (exist != null) {
            throw new BusinessException(409, "该邮箱已被注册");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());

        String token = generateToken();
        user.setToken(token);

        userMapper.insert(user);

        return toLoginResponse(user, token);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectByEmail(req.getEmail());
        if (user == null) {
            throw new BusinessException(401, "邮箱或密码错误");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "邮箱或密码错误");
        }

        String token = generateToken();
        userMapper.updateToken(user.getId(), token);

        return toLoginResponse(user, token);
    }

    public User getUserByToken(String token) {
        User user = userMapper.selectByToken(token);
        if (user == null) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        return user;
    }

    public LoginResponse getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toLoginResponse(user, user.getToken());
    }

    @Transactional
    public LoginResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            User exist = userMapper.selectByEmail(req.getEmail());
            if (exist != null) {
                throw new BusinessException(409, "该邮箱已被其他用户使用");
            }
        }

        User update = new User();
        update.setId(userId);
        update.setName(req.getName());
        update.setEmail(req.getEmail());
        update.setAvatar(req.getAvatar());
        userMapper.updateById(update);

        User updated = userMapper.selectById(userId);
        return toLoginResponse(updated, updated.getToken());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(req.getNewPassword()));
    }

    private LoginResponse toLoginResponse(User user, String token) {
        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatar(),
                token
        );
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
