package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.common.enums.OrderStatus;
import com.sky.common.exception.BusinessException;
import com.sky.dto.ReportQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.DateCountVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.SalesTop10VO;
import com.sky.vo.TurnoverVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表实现。
 *
 * <p>聚合在应用层完成：先用订单主键区间把订单查出来，再按天 / 按商品名归并。
 * 这样同一套代码在 MySQL 和 H2 测试库上行为一致（DATE_FORMAT 在 H2 不兼容），
 * 演示数据量也足够；后续数据量上来可以把聚合下推到 Mapper 的 SQL。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    /** 区间默认 7 天，最长 90 天，避免前端传超大区间全表扫描 */
    private static final int DEFAULT_RANGE_DAYS = 7;
    private static final int MAX_RANGE_DAYS = 90;

    /** 销量 TOP10 的条数 */
    private static final int TOP_LIMIT = 10;

    /** 明细 IN 查询的分片大小，避免 IN 列表过长 */
    private static final int DETAIL_CHUNK_SIZE = 500;

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;

    @Override
    public List<TurnoverVO> turnover(ReportQueryDTO dto) {
        List<LocalDate> dates = resolveDates(dto);

        Map<LocalDate, Long> turnoverByDate = new HashMap<>();
        for (Orders order : selectCompletedOrders(dates)) {
            if (order.getOrderTime() == null) {
                continue;
            }
            long amount = order.getAmount() == null ? 0L : order.getAmount();
            turnoverByDate.merge(order.getOrderTime().toLocalDate(), amount, Long::sum);
        }

        return dates.stream()
                .map(date -> new TurnoverVO(date, turnoverByDate.getOrDefault(date, 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public OrderStatisticsVO orderStatistics(ReportQueryDTO dto) {
        List<LocalDate> dates = resolveDates(dto);

        Map<LocalDate, Long> totalByDate = new HashMap<>();
        Map<LocalDate, Long> validByDate = new HashMap<>();
        for (Orders order : selectOrders(dates, null)) {
            if (order.getOrderTime() == null) {
                continue;
            }
            LocalDate date = order.getOrderTime().toLocalDate();
            totalByDate.merge(date, 1L, Long::sum);
            if (Integer.valueOf(OrderStatus.COMPLETED.getCode()).equals(order.getStatus())) {
                validByDate.merge(date, 1L, Long::sum);
            }
        }

        OrderStatisticsVO statistics = new OrderStatisticsVO();
        statistics.setOrderCountList(dates.stream()
                .map(date -> new DateCountVO(date, totalByDate.getOrDefault(date, 0L), null))
                .collect(Collectors.toList()));
        statistics.setValidOrderCountList(dates.stream()
                .map(date -> new DateCountVO(date, null, validByDate.getOrDefault(date, 0L)))
                .collect(Collectors.toList()));
        return statistics;
    }

    @Override
    public List<SalesTop10VO> salesTop10(ReportQueryDTO dto) {
        List<LocalDate> dates = resolveDates(dto);

        List<Long> orderIds = selectCompletedOrders(dates).stream()
                .map(Orders::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 按商品名称聚合销量：菜品与套餐同名时合并为一条，与接口文档的 {name, sales} 口径一致。
        Map<String, Long> salesByName = new LinkedHashMap<>();
        for (int start = 0; start < orderIds.size(); start += DETAIL_CHUNK_SIZE) {
            List<Long> chunk = orderIds.subList(start, Math.min(start + DETAIL_CHUNK_SIZE, orderIds.size()));
            List<OrderDetail> details = orderDetailMapper.selectList(Wrappers.<OrderDetail>lambdaQuery()
                    .select(OrderDetail::getName, OrderDetail::getNumber)
                    .in(OrderDetail::getOrderId, chunk));
            for (OrderDetail detail : details) {
                long number = detail.getNumber() == null ? 1L : detail.getNumber();
                salesByName.merge(detail.getName(), number, Long::sum);
            }
        }

        return salesByName.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_LIMIT)
                .map(entry -> new SalesTop10VO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 统计区间内的订单，status 为 null 时不限制状态。只取报表需要的列。
     */
    private List<Orders> selectOrders(List<LocalDate> dates, Integer status) {
        LocalDateTime begin = dates.get(0).atStartOfDay();
        LocalDateTime endExclusive = dates.get(dates.size() - 1).plusDays(1).atStartOfDay();
        return orderMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .select(Orders::getId, Orders::getStatus, Orders::getAmount, Orders::getOrderTime)
                .eq(status != null, Orders::getStatus, status)
                .ge(Orders::getOrderTime, begin)
                .lt(Orders::getOrderTime, endExclusive)
                .orderByAsc(Orders::getOrderTime));
    }

    private List<Orders> selectCompletedOrders(List<LocalDate> dates) {
        return selectOrders(dates, OrderStatus.COMPLETED.getCode());
    }

    /**
     * 解析统计区间：begin、end 可缺省，缺省时取最近 7 天；区间顺序必须正确且不超过 90 天。
     */
    private List<LocalDate> resolveDates(ReportQueryDTO dto) {
        LocalDate end = dto == null || dto.getEnd() == null ? LocalDate.now() : dto.getEnd();
        LocalDate begin = dto == null || dto.getBegin() == null
                ? end.minusDays(DEFAULT_RANGE_DAYS - 1L)
                : dto.getBegin();

        if (begin.isAfter(end)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        long days = ChronoUnit.DAYS.between(begin, end) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BusinessException("统计区间最多支持 " + MAX_RANGE_DAYS + " 天");
        }

        List<LocalDate> dates = new ArrayList<>((int) days);
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }
}
