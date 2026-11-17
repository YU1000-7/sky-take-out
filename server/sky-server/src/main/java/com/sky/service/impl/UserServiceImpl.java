package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.common.utils.JwtUtil;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO login(UserLoginDTO dto) {
        String phone = dto.getPhone().trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)
                .last("limit 1"));

        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setName("用户" + phone.substring(phone.length() - 4));
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException exception) {
                user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                        .eq(User::getPhone, phone)
                        .last("limit 1"));
            }
        }

        UserLoginVO result = UserLoginVO.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .token(jwtUtil.createUserToken(user.getId()))
                .build();
        return result;
    }
}
