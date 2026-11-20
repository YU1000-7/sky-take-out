package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.common.exception.BusinessException;
import com.sky.common.utils.JwtUtil;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO login(UserLoginDTO dto) {
        String phone = dto.getPhone().trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)
                .last("limit 1"));

        if (user == null) {
            user = register(phone, dto.getPassword());
        } else if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("手机号或密码错误");
        }

        return UserLoginVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .token(jwtUtil.createUserToken(user.getId()))
                .build();
    }

    private User register(String phone, String rawPassword) {
        User user = new User();
        user.setPhone(phone);
        user.setName("用户" + phone.substring(phone.length() - 4));
        user.setPassword(passwordEncoder.encode(rawPassword));
        try {
            userMapper.insert(user);
            return user;
        } catch (DuplicateKeyException exception) {
            // 并发注册同一手机号时，回查并校验密码，避免覆盖他人账号
            User existing = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getPhone, phone)
                    .last("limit 1"));
            if (existing == null || !passwordEncoder.matches(rawPassword, existing.getPassword())) {
                throw new BusinessException("手机号或密码错误");
            }
            return existing;
        }
    }
}
