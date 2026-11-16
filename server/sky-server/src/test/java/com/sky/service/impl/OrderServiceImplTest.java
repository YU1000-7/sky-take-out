package com.sky.service.impl;

import com.sky.common.context.BaseContext;
import com.sky.common.enums.OrderStatus;
import com.sky.common.exception.BusinessException;
import com.sky.dto.OrderStatusDTO;
import com.sky.dto.OrderSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.GoodsQueryMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.vo.GoodsSnapshotVO;
import com.sky.vo.OrderAdminVO;
import com.sky.vo.OrderSubmitVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单业务单元测试：下单快照与清空购物车、状态机流转、越权访问拦截。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final Long USER_ID = 9L;
    private static final Long ADDRESS_ID = 7L;

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private AddressBookMapper addressBookMapper;
    @Mock
    private GoodsQueryMapper goodsQueryMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void shouldSubmitOrderWithSnapshotAndClearCart() {
        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(dishCart(2, 3800L)));
        when(addressBookMapper.selectById(ADDRESS_ID)).thenReturn(address(USER_ID));
        when(goodsQueryMapper.findDish(1L)).thenReturn(goods(1L, "鱼香肉丝", 3800L, 1));
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.insert(any(Orders.class))).thenAnswer(invocation -> {
            Orders order = invocation.getArgument(0);
            order.setId(101L);
            return 1;
        });

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressBookId(ADDRESS_ID);
        dto.setPayMethod(1);
        dto.setRemark("  不要香菜  ");

        OrderSubmitVO result = orderService.submit(dto);

        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Orders saved = orderCaptor.getValue();
        assertEquals(OrderStatus.PENDING.getCode(), saved.getStatus().intValue(), "新订单进入待接单");
        assertEquals(1, saved.getPayStatus().intValue(), "模拟支付下单即视为已支付");
        assertEquals(7600L, saved.getAmount().longValue(), "金额按 number * amount 汇总");
        assertEquals(USER_ID.longValue(), saved.getUserId().longValue());
        assertEquals("张三", saved.getConsignee(), "收货人写入订单快照");
        assertEquals("13800138000", saved.getPhone(), "电话写入订单快照");
        assertEquals("江苏省苏州市姑苏区观前街1号", saved.getAddress(), "地址写入订单快照");
        assertEquals("不要香菜", saved.getRemark(), "备注需要去掉首尾空格");
        assertNotNull(saved.getOrderTime());
        assertTrue(saved.getNumber().startsWith(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)),
                "订单号以 yyyyMMdd 开头");

        ArgumentCaptor<OrderDetail> detailCaptor = ArgumentCaptor.forClass(OrderDetail.class);
        verify(orderDetailMapper).insert(detailCaptor.capture());
        OrderDetail detail = detailCaptor.getValue();
        assertEquals(101L, detail.getOrderId().longValue());
        assertEquals(1L, detail.getDishId().longValue());
        assertEquals("微辣", detail.getDishFlavor());
        assertEquals(2, detail.getNumber().intValue());
        assertEquals(3800L, detail.getAmount().longValue());

        verify(shoppingCartMapper).delete(any());
        assertEquals(101L, result.getOrderId().longValue());
        assertEquals(7600L, result.getAmount().longValue());
    }

    @Test
    void shouldSubmitSetmealOrderAsSetmealDetail() {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(2L);
        cart.setSetmealId(5L);
        cart.setName("双人特惠餐");
        cart.setNumber(1);
        cart.setAmount(6800L);

        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(cart));
        when(addressBookMapper.selectById(ADDRESS_ID)).thenReturn(address(USER_ID));
        when(goodsQueryMapper.findSetmeal(5L)).thenReturn(goods(5L, "双人特惠餐", 6800L, 1));
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(orderMapper.insert(any(Orders.class))).thenAnswer(invocation -> {
            Orders order = invocation.getArgument(0);
            order.setId(102L);
            return 1;
        });

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressBookId(ADDRESS_ID);
        dto.setPayMethod(1);
        orderService.submit(dto);

        ArgumentCaptor<OrderDetail> detailCaptor = ArgumentCaptor.forClass(OrderDetail.class);
        verify(orderDetailMapper).insert(detailCaptor.capture());
        assertEquals(5L, detailCaptor.getValue().getSetmealId().longValue(), "套餐明细写 setmealId");
        assertNull(detailCaptor.getValue().getDishId(), "套餐明细不写 dishId");
    }

    @Test
    void shouldRejectSubmitWhenCartEmpty() {
        when(shoppingCartMapper.selectList(any())).thenReturn(Collections.emptyList());

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressBookId(ADDRESS_ID);
        dto.setPayMethod(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.submit(dto));
        assertEquals("购物车为空，请先选购商品", exception.getMessage());
        verify(orderMapper, never()).insert(any(Orders.class));
    }

    @Test
    void shouldRejectSubmitWhenAddressBelongsToOthers() {
        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(dishCart(1, 3800L)));
        when(addressBookMapper.selectById(ADDRESS_ID)).thenReturn(address(99L));

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressBookId(ADDRESS_ID);
        dto.setPayMethod(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.submit(dto));
        assertEquals("无权使用该收货地址", exception.getMessage());
        verify(orderMapper, never()).insert(any(Orders.class));
    }

    @Test
    void shouldRejectSubmitWhenDishStopped() {
        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(dishCart(1, 3800L)));
        when(addressBookMapper.selectById(ADDRESS_ID)).thenReturn(address(USER_ID));
        when(goodsQueryMapper.findDish(1L)).thenReturn(goods(1L, "停售小炒", 3800L, 0));

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setAddressBookId(ADDRESS_ID);
        dto.setPayMethod(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.submit(dto));
        assertEquals("菜品「停售小炒」已停售，请移出购物车后重新下单", exception.getMessage());
        verify(orderMapper, never()).insert(any(Orders.class));
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Orders order = order(1L, OrderStatus.CONFIRMED.getCode());
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.confirm(1L));
        assertEquals("订单当前状态为「已接单」，无法接单", exception.getMessage());
        verify(orderMapper, never()).updateById(any(Orders.class));
    }

    @Test
    void shouldConfirmDeliveryAndCompleteInOrder() {
        Orders pending = order(1L, OrderStatus.PENDING.getCode());
        when(orderMapper.selectById(1L)).thenReturn(pending);

        orderService.confirm(1L);
        assertEquals(OrderStatus.CONFIRMED.getCode(), pending.getStatus().intValue());

        orderService.delivery(1L);
        assertEquals(OrderStatus.DELIVERING.getCode(), pending.getStatus().intValue());
        assertEquals(1, pending.getDeliveryStatus().intValue());
        assertNotNull(pending.getDeliveryTime());

        orderService.complete(1L);
        assertEquals(OrderStatus.COMPLETED.getCode(), pending.getStatus().intValue());
        verify(orderMapper, times(3)).updateById(pending);
    }

    @Test
    void shouldRejectCompleteBeforeDelivering() {
        Orders pending = order(1L, OrderStatus.PENDING.getCode());
        when(orderMapper.selectById(1L)).thenReturn(pending);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.complete(1L));
        assertEquals("订单当前状态为「待接单」，无法完成", exception.getMessage());
    }

    @Test
    void shouldCancelPendingOrderWithReason() {
        Orders pending = order(2L, OrderStatus.PENDING.getCode());
        when(orderMapper.selectById(2L)).thenReturn(pending);

        orderService.cancel(2L);

        assertEquals(OrderStatus.CANCELLED.getCode(), pending.getStatus().intValue());
        assertEquals("用户取消订单", pending.getCancelReason());
        assertNotNull(pending.getCancelTime());
        verify(orderMapper).updateById(pending);
    }

    @Test
    void shouldRejectCancelWhenMerchantConfirmed() {
        Orders confirmed = order(2L, OrderStatus.CONFIRMED.getCode());
        when(orderMapper.selectById(2L)).thenReturn(confirmed);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.cancel(2L));
        assertEquals("订单当前状态为「已接单」，无法取消", exception.getMessage());
        verify(orderMapper, never()).updateById(any(Orders.class));
    }

    @Test
    void shouldRejectCancelForOtherUsersOrder() {
        Orders others = order(3L, OrderStatus.PENDING.getCode());
        others.setUserId(99L);
        when(orderMapper.selectById(3L)).thenReturn(others);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.cancel(3L));
        assertEquals("无权操作该订单", exception.getMessage());
    }

    @Test
    void shouldReturnUserDetailWithDetailList() {
        Orders pending = order(4L, OrderStatus.PENDING.getCode());
        when(orderMapper.selectById(4L)).thenReturn(pending);

        OrderDetail detail = new OrderDetail();
        detail.setId(1L);
        detail.setOrderId(4L);
        detail.setName("鱼香肉丝");
        detail.setDishId(1L);
        detail.setNumber(2);
        detail.setAmount(3800L);
        when(orderDetailMapper.selectList(any())).thenReturn(List.of(detail));

        OrderAdminVO result = orderService.userDetail(4L);

        assertEquals("202611040001", result.getOrder().getNumber());
        assertEquals(1, result.getOrderDetailList().size());
        assertEquals("鱼香肉丝", result.getOrderDetailList().get(0).getName());
        assertEquals(3800L, result.getOrderDetailList().get(0).getAmount().longValue());
    }

    @Test
    void shouldRejectUserDetailForOtherUsersOrder() {
        Orders others = order(5L, OrderStatus.PENDING.getCode());
        others.setUserId(99L);
        when(orderMapper.selectById(5L)).thenReturn(others);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.userDetail(5L));
        assertEquals("无权查看该订单", exception.getMessage());
    }

    @Test
    void shouldRejectTransferToUnsupportedStatus() {
        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setId(1L);
        dto.setStatus(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> orderService.transfer(dto));
        assertTrue(exception.getMessage().contains("商家端只支持流转到"));
    }

    @Test
    void shouldTransferToConfirmedThroughUnifiedEntry() {
        Orders pending = order(6L, OrderStatus.PENDING.getCode());
        when(orderMapper.selectById(6L)).thenReturn(pending);

        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setId(6L);
        dto.setStatus(2);
        orderService.transfer(dto);

        assertEquals(OrderStatus.CONFIRMED.getCode(), pending.getStatus().intValue());
    }

    private ShoppingCart dishCart(int number, long amount) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(USER_ID);
        cart.setDishId(1L);
        cart.setDishFlavor("微辣");
        cart.setName("鱼香肉丝");
        cart.setImage("http://example.com/1.jpg");
        cart.setNumber(number);
        cart.setAmount(amount);
        return cart;
    }

    private AddressBook address(Long userId) {
        AddressBook addressBook = new AddressBook();
        addressBook.setId(ADDRESS_ID);
        addressBook.setUserId(userId);
        addressBook.setConsignee("张三");
        addressBook.setPhone("13800138000");
        addressBook.setProvinceName("江苏省");
        addressBook.setCityName("苏州市");
        addressBook.setDistrictName("姑苏区");
        addressBook.setDetail("观前街1号");
        return addressBook;
    }

    private GoodsSnapshotVO goods(Long id, String name, Long price, Integer status) {
        GoodsSnapshotVO goods = new GoodsSnapshotVO();
        goods.setId(id);
        goods.setName(name);
        goods.setPrice(price);
        goods.setStatus(status);
        goods.setImage("http://example.com/" + id + ".jpg");
        return goods;
    }

    private Orders order(Long id, Integer status) {
        Orders order = new Orders();
        order.setId(id);
        order.setNumber("202611040001");
        order.setStatus(status);
        order.setUserId(USER_ID);
        order.setAmount(7600L);
        return order;
    }
}
