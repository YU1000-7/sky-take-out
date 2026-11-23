package com.sky.controller;

import com.sky.common.result.PageResult;
import com.sky.common.result.Result;
import com.sky.dto.OrderPageQueryDTO;
import com.sky.dto.OrderStatusDTO;
import com.sky.service.OrderService;
import com.sky.vo.OrderAdminVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "B端订单接口")
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @ApiOperation("后台订单分页查询")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(OrderPageQueryDTO dto) {
        return Result.success(orderService.adminPage(dto));
    }

    @ApiOperation("查看订单详情")
    @GetMapping("/details/{id}")
    public Result<OrderAdminVO> details(@PathVariable Long id) {
        return Result.success(orderService.adminDetail(id));
    }

    @ApiOperation("商家接单")
    @PutMapping("/confirm")
    public Result<Void> confirm(@RequestParam Long id) {
        orderService.confirm(id);
        return Result.success("接单成功", null);
    }

    @ApiOperation("商家派送")
    @PutMapping("/delivery")
    public Result<Void> delivery(@RequestParam Long id) {
        orderService.delivery(id);
        return Result.success("派送成功", null);
    }

    @ApiOperation("商家完成订单")
    @PutMapping("/complete")
    public Result<Void> complete(@RequestParam Long id) {
        orderService.complete(id);
        return Result.success("订单完成", null);
    }

    @ApiOperation("商家拒单")
    @PutMapping("/rejection")
    public Result<Void> rejection(@RequestParam Long id,
                                  @RequestParam(required = false) String rejectionReason) {
        orderService.reject(id, rejectionReason);
        return Result.success("拒单成功", null);
    }

    @ApiOperation("统一订单状态流转（接单/派送/完成）")
    @PutMapping("/status")
    public Result<Void> transfer(@Valid @RequestBody OrderStatusDTO dto) {
        orderService.transfer(dto);
        return Result.success("状态修改成功", null);
    }
}
