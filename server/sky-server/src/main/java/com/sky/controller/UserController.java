package com.sky.controller;

import com.sky.common.result.Result;
import com.sky.dto.UserLoginDTO;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "C端用户接口")
@RestController
@RequestMapping("/user/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @ApiOperation("客户手机号登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return Result.success("登录成功", userService.login(dto));
    }
}
