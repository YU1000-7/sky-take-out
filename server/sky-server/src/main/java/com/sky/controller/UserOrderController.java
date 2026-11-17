package com.sky.controller;

import com.sky.common.result.PageResult;
import com.sky.common.result.Result;
import com.sky.dto.OrderPageQueryDTO;
import com.sky.dto.OrderSubmitDTO;
import com.sky.service.OrderService;
import com.sky.vo.OrderAdminVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "C端订单接口")
@RestController
@RequestMapping("/user/order")
@RequiredArgsConstructor
@Validated
public class UserOrderController {

    private final OrderService orderService;

    @ApiOperation("提交订单")
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        return Result.success("下单成功", orderService.submit(dto));
    }

    @ApiOperation("用户订单分页查询")
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> list(OrderPageQueryDTO dto) {
        return Result.success(orderService.userPage(dto));
    }

    @ApiOperation("查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderAdminVO> detail(@PathVariable Long id) {
        return Result.success(orderService.userDetail(id));
    }

    @ApiOperation("用户取消订单")
    @PutMapping("/cancel")
    public Result<Void> cancel(@RequestParam Long id) {
        orderService.cancel(id);
        return Result.success("取消订单成功", null);
    }
}
