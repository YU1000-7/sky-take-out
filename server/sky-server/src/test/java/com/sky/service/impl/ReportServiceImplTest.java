package com.sky.service.impl;

import com.sky.common.enums.OrderStatus;
import com.sky.common.exception.BusinessException;
import com.sky.dto.ReportQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.SalesTop10VO;
import com.sky.vo.TurnoverVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报表业务单元测试：按天聚合、补零、TOP10 排序和区间校验。
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate DAY_2 = LocalDate.of(2026, 11, 2);
    private static final LocalDate DAY_3 = LocalDate.of(2026, 11, 3);

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void shouldSumTurnoverByDayAndFillMissingDates() {
        when(orderMapper.selectList(any())).thenReturn(List.of(
                order(1L, OrderStatus.COMPLETED.getCode(), DAY_1.atTime(10, 0), 1200L),
                order(2L, OrderStatus.COMPLETED.getCode(), DAY_1.atTime(18, 30), 800L)));

        List<TurnoverVO> result = reportService.turnover(query(DAY_1, DAY_3));

        assertEquals(3, result.size(), "区间内每一天都要出现");
        assertEquals(DAY_1, result.get(0).getDate());
        assertEquals(2000L, result.get(0).getTurnover().longValue(), "同一天多笔订单需要累加");
        assertEquals(0L, result.get(1).getTurnover().longValue(), "没有订单的日期补 0");
        assertEquals(DAY_3, result.get(2).getDate());
        assertEquals(0L, result.get(2).getTurnover().longValue());
    }

    @Test
    void shouldCountTotalOrdersAndValidOrders() {
        when(orderMapper.selectList(any())).thenReturn(List.of(
                order(1L, OrderStatus.COMPLETED.getCode(), DAY_1.atTime(9, 0), 1000L),
                order(2L, OrderStatus.PENDING.getCode(), DAY_1.atTime(12, 0), 500L),
                order(3L, OrderStatus.CANCELLED.getCode(), DAY_1.atTime(13, 0), 300L),
                order(4L, OrderStatus.COMPLETED.getCode(), DAY_2.atTime(9, 0), 200L),
                order(5L, OrderStatus.PENDING.getCode(), DAY_2.atTime(10, 0), 600L)));

        OrderStatisticsVO statistics = reportService.orderStatistics(query(DAY_1, DAY_2));

        assertEquals(2, statistics.getOrderCountList().size());
        assertEquals(3L, statistics.getOrderCountList().get(0).getOrderCount().longValue());
        assertNull(statistics.getOrderCountList().get(0).getValidOrderCount(), "订单总数列表只填 orderCount");
        assertEquals(2L, statistics.getOrderCountList().get(1).getOrderCount().longValue());

        assertEquals(1L, statistics.getValidOrderCountList().get(0).getValidOrderCount().longValue());
        assertNull(statistics.getValidOrderCountList().get(0).getOrderCount(), "有效订单列表只填 validOrderCount");
        assertEquals(1L, statistics.getValidOrderCountList().get(1).getValidOrderCount().longValue());
    }

    @Test
    void shouldRankTop10BySalesName() {
        when(orderMapper.selectList(any())).thenReturn(List.of(
                order(1L, OrderStatus.COMPLETED.getCode(), DAY_1.atTime(10, 0), 1000L),
                order(2L, OrderStatus.COMPLETED.getCode(), DAY_1.atTime(11, 0), 2000L)));
        when(orderDetailMapper.selectList(any())).thenReturn(List.of(
                detail("鱼香肉丝", 2),
                detail("宫保鸡丁", 5),
                detail("鱼香肉丝", 1)));

        List<SalesTop10VO> result = reportService.salesTop10(query(DAY_1, DAY_1));

        assertEquals(2, result.size());
        assertEquals("宫保鸡丁", result.get(0).getName(), "销量高的排前面");
        assertEquals(5L, result.get(0).getSales().longValue());
        assertEquals("鱼香肉丝", result.get(1).getName());
        assertEquals(3L, result.get(1).getSales().longValue(), "同名商品销量合并");
    }

    @Test
    void shouldSkipDetailQueryWhenNoCompletedOrder() {
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SalesTop10VO> result = reportService.salesTop10(query(DAY_1, DAY_2));

        assertTrue(result.isEmpty());
        verify(orderDetailMapper, never()).selectList(any());
    }

    @Test
    void shouldDefaultToLastSevenDays() {
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<TurnoverVO> result = reportService.turnover(new ReportQueryDTO());

        assertEquals(7, result.size());
        assertEquals(LocalDate.now(), result.get(6).getDate());
        assertEquals(LocalDate.now().minusDays(6), result.get(0).getDate());
    }

    @Test
    void shouldRejectReversedRange() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reportService.turnover(query(DAY_2, DAY_1)));
        assertEquals("开始日期不能晚于结束日期", exception.getMessage());
    }

    @Test
    void shouldRejectRangeLongerThanNinetyDays() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> reportService.orderStatistics(query(DAY_1, DAY_1.plusDays(90))));
        assertEquals("统计区间最多支持 90 天", exception.getMessage());
    }

    private ReportQueryDTO query(LocalDate begin, LocalDate end) {
        ReportQueryDTO dto = new ReportQueryDTO();
        dto.setBegin(begin);
        dto.setEnd(end);
        return dto;
    }

    private Orders order(Long id, Integer status, LocalDateTime orderTime, Long amount) {
        Orders order = new Orders();
        order.setId(id);
        order.setNumber("20261104000" + id);
        order.setStatus(status);
        order.setAmount(amount);
        order.setOrderTime(orderTime);
        return order;
    }

    private OrderDetail detail(String name, int number) {
        OrderDetail detail = new OrderDetail();
        detail.setName(name);
        detail.setNumber(number);
        detail.setAmount(1000L);
        return detail;
    }
}
