package com.sky.controller;

import com.sky.common.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealDishDetailVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "C端套餐查询接口")
@RestController
@RequestMapping("/user/setmeal")
@RequiredArgsConstructor
public class UserSetmealController {

    private final SetmealService setmealService;

    @ApiOperation("根据分类ID查询起售套餐")
    @GetMapping("/list")
    public Result<List<SetmealVO>> list(@RequestParam(required = false) Long categoryId) {
        return Result.success(setmealService.listOnSaleByCategory(categoryId));
    }

    @ApiOperation("查询套餐内菜品")
    @GetMapping("/dish/{id}")
    public Result<List<SetmealDishDetailVO>> listDish(@PathVariable Long id) {
        return Result.success(setmealService.listDishBySetmealId(id));
    }
}
