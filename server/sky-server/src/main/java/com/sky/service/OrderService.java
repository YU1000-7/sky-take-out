package com.sky.service;

import com.sky.common.result.PageResult;
import com.sky.dto.OrderPageQueryDTO;
import com.sky.dto.OrderStatusDTO;
import com.sky.dto.OrderSubmitDTO;
import com.sky.vo.OrderAdminVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

/**
 * 订单业务：客户下单 / 查询 / 取消，商家查询 / 接单 / 派送 / 完成。
 *
 * <p>状态机：1 待接单 → 2 已接单 → 3 派送中 → 4 已完成；1 待接单 → 5 已取消。
 * 所有状态变更都必须校验当前状态，非法流转返回业务异常。</p>
 */
public interface OrderService {

    /**
     * C 端：提交订单。购物车、订单主表、订单明细和清空购物车在同一事务内完成。
     */
    OrderSubmitVO submit(OrderSubmitDTO dto);

    /**
     * B 端：订单分页查询，支持订单号、状态和下单时间区间筛选。
     */
    PageResult<OrderVO> adminPage(OrderPageQueryDTO dto);

    /**
     * B 端：订单详情，返回订单信息与明细列表。
     */
    OrderAdminVO adminDetail(Long id);

    /**
     * C 端：当前客户的订单分页查询，只能看到自己的订单。
     */
    PageResult<OrderVO> userPage(OrderPageQueryDTO dto);

    /**
     * C 端：当前客户的订单详情，非本人订单拒绝访问。
     */
    OrderAdminVO userDetail(Long id);

    /**
     * B 端：接单，1 待接单 → 2 已接单。
     */
    void confirm(Long id);

    /**
     * B 端：派送，2 已接单 → 3 派送中。
     */
    void delivery(Long id);

    /**
     * B 端：完成订单，3 派送中 → 4 已完成。
     */
    void complete(Long id);

    /**
     * C 端：取消订单，只有 1 待接单可以取消。
     */
    void cancel(Long id);

    /**
     * B 端：统一状态流转入口，供接单、派送、完成复用同一套状态校验。
     */
    void transfer(OrderStatusDTO dto);
}
