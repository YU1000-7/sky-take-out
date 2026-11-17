package com.sky.controller;

import com.sky.common.result.Result;
import com.sky.dto.AddressBookDTO;
import com.sky.service.AddressBookService;
import com.sky.vo.AddressBookVO;
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
import java.util.List;

@Api(tags = "C端地址簿接口")
@RestController
@RequestMapping("/user/addressBook")
@RequiredArgsConstructor
@Validated
public class AddressBookController {

    private final AddressBookService addressBookService;

    @ApiOperation("新增地址")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody AddressBookDTO dto) {
        addressBookService.add(dto);
        return Result.success("新增成功", null);
    }

    @ApiOperation("修改地址")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AddressBookDTO dto) {
        addressBookService.update(dto);
        return Result.success("修改成功", null);
    }

    @ApiOperation("删除地址")
    @DeleteMapping
    public Result<Void> delete(@RequestParam Long id) {
        addressBookService.delete(id);
        return Result.success("删除成功", null);
    }

    @ApiOperation("查询当前用户地址列表")
    @GetMapping("/list")
    public Result<List<AddressBookVO>> list() {
        return Result.success(addressBookService.list());
    }

    @ApiOperation("查询单条地址")
    @GetMapping("/{id}")
    public Result<AddressBookVO> getById(@PathVariable Long id) {
        return Result.success(addressBookService.getById(id));
    }

    @ApiOperation("设置默认地址")
    @PutMapping("/default")
    public Result<Void> setDefault(@RequestParam Long id) {
        addressBookService.setDefault(id);
        return Result.success("设置默认地址成功", null);
    }
}
