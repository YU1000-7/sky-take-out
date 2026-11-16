package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.common.context.BaseContext;
import com.sky.common.enums.OrderStatus;
import com.sky.common.exception.BusinessException;
import com.sky.common.result.PageResult;
import com.sky.dto.OrderPageQueryDTO;
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
import com.sky.service.OrderService;
import com.sky.vo.GoodsSnapshotVO;
import com.sky.vo.OrderAdminVO;
import com.sky.vo.OrderDetailVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 默认分页大小与最大分页大小，避免前端传入异常值拖垮数据库 */
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /** 模拟支付：下单即置为已支付，演示环境不接真实支付渠道 */
    private static final Integer PAY_STATUS_PAID = 1;
    /** 配送状态：派送中 */
    private static final Integer DELIVERY_STATUS_DELIVERING = 1;
    /** 商品起售状态 */
    private static final Integer GOODS_ON_SALE = 1;

    /** 预计送达时间：下单后 30 分钟 */
    private static final int ESTIMATE_DELIVERY_MINUTES = 30;

    /** 客户取消订单时写入的原因 */
    private static final String USER_CANCEL_REASON = "用户取消订单";

    private static final DateTimeFormatter ORDER_NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final AddressBookMapper addressBookMapper;
    /**
     * 读取 dish / setmeal 的只读适配层，复用 C 模块的 GoodsQueryMapper，
     * 避免与 B 的 DishMapper 产生实体和 Mapper 重名冲突。
     */
    private final GoodsQueryMapper goodsQueryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrderSubmitDTO dto) {
        Long userId = requireUserId();

        List<ShoppingCart> carts = shoppingCartMapper.selectList(Wrappers.<ShoppingCart>lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .orderByAsc(ShoppingCart::getId));
        if (carts.isEmpty()) {
            throw new BusinessException("购物车为空，请先选购商品");
        }

        AddressBook addressBook = requireAddress(dto.getAddressBookId(), userId);

        // 加购后商品可能被停售或删除，下单前再校验一次，避免卖出已下架商品。
        ensureGoodsOnSale(carts);

        long amount = 0L;
        for (ShoppingCart cart : carts) {
            amount += subtotal(cart);
        }
        if (amount <= 0) {
            throw new BusinessException("订单金额异常，请重新选择商品");
        }

        LocalDateTime now = LocalDateTime.now();
        Orders order = new Orders();
        order.setNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setUserId(userId);
        order.setAddressBookId(addressBook.getId());
        order.setPayStatus(PAY_STATUS_PAID);
        order.setPayMethod(dto.getPayMethod());
        order.setAmount(amount);
        order.setRemark(normalizeText(dto.getRemark()));
        // 收货人、电话、地址写入订单快照，后续客户改地址不影响历史订单。
        order.setConsignee(addressBook.getConsignee());
        order.setPhone(addressBook.getPhone());
        order.setAddress(buildAddress(addressBook));
        order.setOrderTime(now);
        order.setCheckoutTime(now);
        order.setPackAmount(0L);
        order.setEstimateDeliveryTime(now.plusMinutes(ESTIMATE_DELIVERY_MINUTES));
        orderMapper.insert(order);

        for (ShoppingCart cart : carts) {
            orderDetailMapper.insert(buildOrderDetail(order.getId(), cart));
        }

        // 下单成功后清空购物车，避免同一批商品被重复提交。
        shoppingCartMapper.delete(Wrappers.<ShoppingCart>lambdaQuery()
                .eq(ShoppingCart::getUserId, userId));

        log.info("客户 {} 下单成功，订单号 {}，金额 {} 分", userId, order.getNumber(), amount);
        return OrderSubmitVO.builder()
                .orderId(order.getId())
                .number(order.getNumber())
                .amount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    @Override
    public PageResult<OrderVO> adminPage(OrderPageQueryDTO dto) {
        Page<Orders> page = orderMapper.selectPage(
                new Page<>(pageNumberOf(dto.getPage()), pageSizeOf(dto.getPageSize())),
                Wrappers.<Orders>lambdaQuery()
                        .like(StringUtils.hasText(dto.getNumber()), Orders::getNumber, dto.getNumber())
                        .eq(dto.getStatus() != null, Orders::getStatus, dto.getStatus())
                        .ge(dto.getBeginTime() != null, Orders::getOrderTime, dto.getBeginTime())
                        .le(dto.getEndTime() != null, Orders::getOrderTime, dto.getEndTime())
                        .orderByDesc(Orders::getOrderTime, Orders::getId)
        );
        return toPageResult(page);
    }

    @Override
    public PageResult<OrderVO> userPage(OrderPageQueryDTO dto) {
        Long userId = requireUserId();
        Page<Orders> page = orderMapper.selectPage(
                new Page<>(pageNumberOf(dto.getPage()), pageSizeOf(dto.getPageSize())),
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getUserId, userId)
                        .eq(dto.getStatus() != null, Orders::getStatus, dto.getStatus())
                        .orderByDesc(Orders::getOrderTime, Orders::getId)
        );
        return toPageResult(page);
    }

    @Override
    public OrderAdminVO adminDetail(Long id) {
        return buildDetail(requireOrder(id));
    }

    @Override
    public OrderAdminVO userDetail(Long id) {
        Long userId = requireUserId();
        Orders order = requireOrder(id);
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权查看该订单");
        }
        return buildDetail(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        Orders order = requireOrder(id);
        changeStatus(order, OrderStatus.PENDING, OrderStatus.CONFIRMED, "接单");
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delivery(Long id) {
        Orders order = requireOrder(id);
        changeStatus(order, OrderStatus.CONFIRMED, OrderStatus.DELIVERING, "派送");
        order.setDeliveryStatus(DELIVERY_STATUS_DELIVERING);
        order.setDeliveryTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        Orders order = requireOrder(id);
        changeStatus(order, OrderStatus.DELIVERING, OrderStatus.COMPLETED, "完成");
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Long userId = requireUserId();
        Orders order = requireOrder(id);
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作该订单");
        }
        changeStatus(order, OrderStatus.PENDING, OrderStatus.CANCELLED, "取消");
        order.setCancelReason(USER_CANCEL_REASON);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(OrderStatusDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("订单 ID 不能为空");
        }
        if (dto.getStatus() == null) {
            throw new BusinessException("目标状态不能为空");
        }
        // 商家端只负责接单、派送、完成三步，取消订单由客户发起。
        switch (dto.getStatus()) {
            case 2:
                confirm(dto.getId());
                return;
            case 3:
                delivery(dto.getId());
                return;
            case 4:
                complete(dto.getId());
                return;
            default:
                throw new BusinessException("商家端只支持流转到已接单、派送中、已完成，取消订单请使用客户取消接口");
        }
    }

    private Orders requireOrder(Long id) {
        if (id == null) {
            throw new BusinessException("订单 ID 不能为空");
        }
        Orders order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在或已被删除");
        }
        return order;
    }

    private AddressBook requireAddress(Long addressBookId, Long userId) {
        if (addressBookId == null) {
            throw new BusinessException("请选择收货地址");
        }
        AddressBook addressBook = addressBookMapper.selectById(addressBookId);
        if (addressBook == null) {
            throw new BusinessException("收货地址不存在，请重新选择");
        }
        if (!userId.equals(addressBook.getUserId())) {
            throw new BusinessException("无权使用该收货地址");
        }
        return addressBook;
    }

    /**
     * 校验购物车商品仍处于起售状态。菜品与套餐分别通过只读适配层查询。
     */
    private void ensureGoodsOnSale(List<ShoppingCart> carts) {
        for (ShoppingCart cart : carts) {
            if (cart.getDishId() != null) {
                GoodsSnapshotVO dish = goodsQueryMapper.findDish(cart.getDishId());
                if (dish == null) {
                    throw new BusinessException("购物车中的菜品「" + cart.getName() + "」已下架，请移出后重新下单");
                }
                if (!GOODS_ON_SALE.equals(dish.getStatus())) {
                    throw new BusinessException("菜品「" + dish.getName() + "」已停售，请移出购物车后重新下单");
                }
                continue;
            }
            if (cart.getSetmealId() != null) {
                GoodsSnapshotVO setmeal = goodsQueryMapper.findSetmeal(cart.getSetmealId());
                if (setmeal == null) {
                    throw new BusinessException("购物车中的套餐「" + cart.getName() + "」已下架，请移出后重新下单");
                }
                if (!GOODS_ON_SALE.equals(setmeal.getStatus())) {
                    throw new BusinessException("套餐「" + setmeal.getName() + "」已停售，请移出购物车后重新下单");
                }
            }
        }
    }

    private long subtotal(ShoppingCart cart) {
        int number = cart.getNumber() == null ? 1 : cart.getNumber();
        long price = cart.getAmount() == null ? 0L : cart.getAmount();
        return (long) number * price;
    }

    /**
     * 生成订单号：yyyyMMdd + 6 位随机序号，先查重再落库，避免撞上唯一索引。
     */
    private String generateOrderNumber() {
        String day = LocalDate.now().format(ORDER_NUMBER_DATE);
        for (int i = 0; i < 5; i++) {
            String candidate = day + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            Long used = orderMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getNumber, candidate));
            if (used == null || used == 0) {
                return candidate;
            }
        }
        throw new BusinessException("订单号生成失败，请稍后重试");
    }

    private OrderDetail buildOrderDetail(Long orderId, ShoppingCart cart) {
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(orderId);
        detail.setName(cart.getName());
        detail.setImage(cart.getImage());
        detail.setDishId(cart.getDishId());
        detail.setSetmealId(cart.getSetmealId());
        detail.setDishFlavor(cart.getDishFlavor());
        detail.setNumber(cart.getNumber() == null ? 1 : cart.getNumber());
        detail.setAmount(cart.getAmount() == null ? 0L : cart.getAmount());
        return detail;
    }

    /**
     * 状态机校验：当前状态不是 from 时拒绝流转，并给出可读提示。
     */
    private void changeStatus(Orders order, OrderStatus from, OrderStatus to, String action) {
        if (!Integer.valueOf(from.getCode()).equals(order.getStatus())) {
            throw new BusinessException("订单当前状态为「" + descriptionOf(order.getStatus())
                    + "」，无法" + action);
        }
        order.setStatus(to.getCode());
    }

    private String descriptionOf(Integer status) {
        if (status == null) {
            return "未知";
        }
        for (OrderStatus item : OrderStatus.values()) {
            if (Integer.valueOf(item.getCode()).equals(status)) {
                return item.getDescription();
            }
        }
        return String.valueOf(status);
    }

    private String buildAddress(AddressBook addressBook) {
        StringBuilder builder = new StringBuilder();
        for (String part : List.of(
                nullToEmpty(addressBook.getProvinceName()),
                nullToEmpty(addressBook.getCityName()),
                nullToEmpty(addressBook.getDistrictName()),
                nullToEmpty(addressBook.getDetail()))) {
            if (StringUtils.hasText(part)) {
                builder.append(part.trim());
            }
        }
        String address = builder.toString();
        return StringUtils.hasText(address) ? address : "-";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int pageNumberOf(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int pageSizeOf(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private PageResult<OrderVO> toPageResult(Page<Orders> page) {
        List<OrderVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    private OrderAdminVO buildDetail(Orders order) {
        List<OrderDetail> details = orderDetailMapper.selectList(Wrappers.<OrderDetail>lambdaQuery()
                .eq(OrderDetail::getOrderId, order.getId())
                .orderByAsc(OrderDetail::getId));

        OrderAdminVO orderAdminVO = new OrderAdminVO();
        orderAdminVO.setOrder(toVO(order));
        orderAdminVO.setOrderDetailList(details.stream()
                .map(this::toDetailVO)
                .collect(Collectors.toList()));
        return orderAdminVO;
    }

    private OrderVO toVO(Orders order) {
        OrderVO orderVO = new OrderVO();
        orderVO.setId(order.getId());
        orderVO.setNumber(order.getNumber());
        orderVO.setStatus(order.getStatus());
        orderVO.setUserId(order.getUserId());
        orderVO.setAddressBookId(order.getAddressBookId());
        orderVO.setOrderTime(order.getOrderTime());
        orderVO.setCheckoutTime(order.getCheckoutTime());
        orderVO.setPayMethod(order.getPayMethod());
        orderVO.setAmount(order.getAmount());
        orderVO.setRemark(order.getRemark());
        orderVO.setConsignee(order.getConsignee());
        orderVO.setPhone(order.getPhone());
        orderVO.setAddress(order.getAddress());
        orderVO.setPayStatus(order.getPayStatus());
        orderVO.setEstimateDeliveryTime(order.getEstimateDeliveryTime());
        orderVO.setDeliveryTime(order.getDeliveryTime());
        orderVO.setCancelReason(order.getCancelReason());
        return orderVO;
    }

    private OrderDetailVO toDetailVO(OrderDetail detail) {
        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setId(detail.getId());
        detailVO.setOrderId(detail.getOrderId());
        detailVO.setName(detail.getName());
        detailVO.setImage(detail.getImage());
        detailVO.setDishId(detail.getDishId());
        detailVO.setSetmealId(detail.getSetmealId());
        detailVO.setDishFlavor(detail.getDishFlavor());
        detailVO.setNumber(detail.getNumber());
        detailVO.setAmount(detail.getAmount());
        return detailVO;
    }

    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已失效，请重新登录");
        }
        return userId;
    }
}
