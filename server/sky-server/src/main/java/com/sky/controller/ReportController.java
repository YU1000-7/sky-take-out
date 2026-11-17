package com.sky.controller;

import com.sky.common.result.Result;
import com.sky.dto.ReportQueryDTO;
import com.sky.service.ReportService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.SalesTop10VO;
import com.sky.vo.TurnoverVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "B端报表统计接口")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @ApiOperation("营业额统计")
    @GetMapping("/turnover")
    public Result<List<TurnoverVO>> turnover(ReportQueryDTO dto) {
        return Result.success(reportService.turnover(dto));
    }

    @ApiOperation("订单统计")
    @GetMapping("/orderStatistics")
    public Result<OrderStatisticsVO> orderStatistics(ReportQueryDTO dto) {
        return Result.success(reportService.orderStatistics(dto));
    }

    @ApiOperation("销量TOP10")
    @GetMapping("/top10")
    public Result<List<SalesTop10VO>> top10(ReportQueryDTO dto) {
        return Result.success(reportService.salesTop10(dto));
    }
}
