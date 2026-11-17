package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 订单明细实体，对应 order_detail 表。
 *
 * <p>dishId 与 setmealId 二选一：菜品明细写 dishId，套餐明细写 setmealId，
 * 名称、图片、单价都是下单时的快照。</p>
 */
@Data
@TableName("order_detail")
public class OrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称快照 */
    private String name;

    private String image;

    private Long orderId;

    private Long dishId;

    private Long setmealId;

    private String dishFlavor;

    private Integer number;

    /** 单价快照，单位分 */
    private Long amount;
}
