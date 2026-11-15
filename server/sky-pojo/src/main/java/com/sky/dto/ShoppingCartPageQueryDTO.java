package com.sky.dto;

import lombok.Data;

/**
 * 购物车分页查询入参。
 * 当前契约 C 端购物车列表不分页，保留该 DTO 供后续按商品类型过滤或后台排查使用。
 */
@Data
public class ShoppingCartPageQueryDTO {

    private Integer page = 1;
    private Integer pageSize = 10;
    private Long userId;
    private Long dishId;
    private Long setmealId;
}
