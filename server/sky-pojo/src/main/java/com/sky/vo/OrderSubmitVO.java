package com.sky.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 下单成功出参。
 *
 * <p>按接口文档 {@code POST /user/order/submit} 返回 {@code {"orderId": 1}}，
 * 这里额外带出订单号、金额和下单时间，前端可以直接跳转订单详情页。</p>
 */
@Data
@Builder
public class OrderSubmitVO implements Serializable {

    private Long orderId;

    private String number;

    /** 订单金额，单位分 */
    private Long amount;

    private LocalDateTime orderTime;
}
