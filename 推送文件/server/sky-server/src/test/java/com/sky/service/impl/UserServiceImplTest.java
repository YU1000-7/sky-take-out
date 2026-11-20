package com.sky.service.impl;

import com.sky.common.exception.BusinessException;
import com.sky.common.utils.JwtUtil;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.vo.UserLoginVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserLoginDTO loginDTO(String phone, String password) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setPhone(phone);
        dto.setPassword(password);
        return dto;
    }

    @Test
    void shouldLoginExistingUserWhenPasswordMatches() {
        User user = new User();
        user.setId(3L);
        user.setPhone("13800138000");
        user.setName("测试用户");
        user.setPassword("encoded-old");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("123456", "encoded-old")).thenReturn(true);
        when(jwtUtil.createUserToken(3L)).thenReturn("user-token");

        UserLoginVO result = userService.login(loginDTO("13800138000", "123456"));

        assertEquals(3L, result.getId());
        assertEquals("user-token", result.getToken());
        assertEquals("测试用户", result.getName());
    }

    @Test
    void shouldRejectExistingUserWhenPasswordMismatch() {
        User user = new User();
        user.setId(3L);
        user.setPhone("13800138000");
        user.setPassword("encoded-old");

        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong-pass", "encoded-old")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.login(loginDTO("13800138000", "wrong-pass")));
        assertEquals("手机号或密码错误", exception.getMessage());
    }

    @Test
    void shouldRegisterNewUserWithEncodedPassword() {
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("abcdef")).thenReturn("encoded-new");

        UserLoginVO result = userService.login(loginDTO("13900139000", "abcdef"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("13900139000", captor.getValue().getPhone());
        assertEquals("encoded-new", captor.getValue().getPassword());
        assertEquals("用户9000", result.getName());
    }
}
