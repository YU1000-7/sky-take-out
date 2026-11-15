package com.sky.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 购物车出参。
 * dishId / setmealId 供 H5 判断商品类型，也是 D 模块提交订单读取购物车的口径。
 */
@Data
@Builder
public class ShoppingCartVO {

    private Long id;
    private Long dishId;
    private Long setmealId;
    private String name;
    private String image;
    private String dishFlavor;
    private Integer number;
    /** 单价快照，单位分 */
    private Long amount;
}
