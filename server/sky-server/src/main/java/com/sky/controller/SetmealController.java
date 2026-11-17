package com.sky.controller;

import com.sky.common.result.PageResult;
import com.sky.common.result.Result;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "B端套餐接口")
@RestController
@RequestMapping("/admin/setmeal")
@RequiredArgsConstructor
@Validated
public class SetmealController {

    private final SetmealService setmealService;

    @ApiOperation("新增套餐")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SetmealDTO dto) {
        setmealService.save(dto);
        return Result.success("新增套餐成功", null);
    }

    @ApiOperation("修改套餐")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SetmealDTO dto) {
        setmealService.update(dto);
        return Result.success("修改成功", null);
    }

    @ApiOperation("删除套餐")
    @DeleteMapping
    public Result<Void> delete(@RequestParam String ids) {
        setmealService.delete(ids);
        return Result.success("删除成功", null);
    }

    @ApiOperation("套餐分页查询")
    @GetMapping("/page")
    public Result<PageResult<SetmealVO>> page(SetmealPageQueryDTO dto) {
        return Result.success(setmealService.page(dto));
    }

    @ApiOperation("根据ID查询套餐（含套餐菜品）")
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        return Result.success(setmealService.getById(id));
    }

    @ApiOperation("套餐起售停售")
    @PutMapping("/status/{status}")
    public Result<Void> startOrStop(@PathVariable Integer status, @RequestParam Long id) {
        setmealService.startOrStop(status, id);
        return Result.success("状态修改成功", null);
    }
}
