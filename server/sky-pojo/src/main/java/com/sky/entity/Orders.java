package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体，对应 orders 表。
 *
 * <p>下单时把收货人、电话、地址和金额一并写入订单快照，
 * 后续客户修改地址或商品改价都不影响历史订单。</p>
 */
@Data
@TableName("orders")
public class Orders {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，yyyyMMdd + 6 位序号 */
    private String number;

    /** 1 待接单，2 已接单，3 派送中，4 已完成，5 已取消 */
    private Integer status;

    private Long userId;

    private Long addressBookId;

    /** 0 未支付，1 已支付，2 退款 */
    private Integer payStatus;

    /** 1 模拟支付 */
    private Integer payMethod;

    /** 订单金额，单位分 */
    private Long amount;

    private String remark;

    /** 联系电话快照 */
    private String phone;

    /** 收货地址快照 */
    private String address;

    /** 收货人快照 */
    private String consignee;

    private LocalDateTime estimateDeliveryTime;

    /** 配送状态，派送中置 1 */
    private Integer deliveryStatus;

    private LocalDateTime orderTime;

    private LocalDateTime checkoutTime;

    private String cancelReason;

    private String rejectionReason;

    private LocalDateTime cancelTime;

    private LocalDateTime deliveryTime;

    /** 打包费，单位分 */
    private Long packAmount;

    private Integer tablewareNumber;

    private Integer tablewareStatus;
}
