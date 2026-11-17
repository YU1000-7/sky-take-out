package com.sky.controller;

import com.sky.common.result.Result;
import com.sky.dto.ShoppingCartDTO;
import com.sky.service.ShoppingCartService;
import com.sky.vo.ShoppingCartVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "C端购物车接口")
@RestController
@RequestMapping("/user/shoppingCart")
@RequiredArgsConstructor
@Validated
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @ApiOperation("添加购物车")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody ShoppingCartDTO dto) {
        shoppingCartService.add(dto);
        return Result.success("添加成功", null);
    }

    @ApiOperation("购物车减少商品")
    @PutMapping("/sub")
    public Result<Void> sub(@Valid @RequestBody ShoppingCartDTO dto) {
        shoppingCartService.sub(dto);
        return Result.success("减少成功", null);
    }

    @ApiOperation("查询购物车列表")
    @GetMapping("/list")
    public Result<List<ShoppingCartVO>> list() {
        return Result.success(shoppingCartService.list());
    }

    @ApiOperation("清空购物车")
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        shoppingCartService.clean();
        return Result.success("清空购物车成功", null);
    }
}
