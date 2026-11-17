package com.sky.service;

import com.sky.dto.ReportQueryDTO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.SalesTop10VO;
import com.sky.vo.TurnoverVO;

import java.util.List;

/**
 * 报表业务：营业额、订单统计、销量 TOP10。
 *
 * <p>统计口径：营业额与销量只统计已完成订单（状态 4）；
 * 订单总数统计区间内全部订单（含已取消），有效订单数统计已完成订单。</p>
 */
public interface ReportService {

    /**
     * B 端：按天统计营业额，区间内没有订单的日期补 0。
     */
    List<TurnoverVO> turnover(ReportQueryDTO dto);

    /**
     * B 端：按天统计订单总数与有效订单数，区间内没有订单的日期补 0。
     */
    OrderStatisticsVO orderStatistics(ReportQueryDTO dto);

    /**
     * B 端：统计区间内已完成订单的销量 TOP10，按商品名称聚合。
     */
    List<SalesTop10VO> salesTop10(ReportQueryDTO dto);
}
