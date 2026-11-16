package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单出参。列表与详情共用。
 *
 * <p>收货人、电话、地址都是下单时的订单快照，不随客户修改地址而变化。
 * 配送与取消相关的扩展字段由 D 模块补充，前端按需展示。</p>
 */
@Data
public class OrderVO implements Serializable {

    private Long id;
    private String number;
    private Integer status;
    private Long userId;
    private Long addressBookId;
    private LocalDateTime orderTime;
    private LocalDateTime checkoutTime;
    private Integer payMethod;
    private Long amount;
    private String remark;
    private String userName;
    private String phone;
    private String address;
    private String consignee;

    /** 0 未支付，1 已支付，2 退款 */
    private Integer payStatus;

    private LocalDateTime estimateDeliveryTime;

    private LocalDateTime deliveryTime;

    /** 取消原因，仅已取消订单有值 */
    private String cancelReason;
}
