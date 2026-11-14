package com.sky.service.impl;

import com.sky.common.utils.JwtUtil;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.vo.UserLoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldLoginExistingUserAndCreateToken() {
        User user = new User();
        user.setId(3L);
        user.setPhone("13800138000");
        user.setName("测试用户");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwtUtil.createUserToken(3L)).thenReturn("user-token");

        UserLoginDTO dto = new UserLoginDTO();
        dto.setPhone("13800138000");
        UserLoginVO result = userService.login(dto);

        assertEquals(3L, result.getId());
        assertEquals("user-token", result.getToken());
        assertEquals("测试用户", result.getName());
    }
}
