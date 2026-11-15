package com.sky.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车实体，对应 shopping_cart 表。
 * dishId 与 setmealId 二选一，name、image、amount 均为加入时的商品快照。
 */
@Data
@TableName("shopping_cart")
public class ShoppingCart {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long dishId;

    private Long setmealId;

    private String dishFlavor;

    private Integer number;

    /** 单价快照，单位分 */
    private Long amount;

    private String name;

    private String image;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
