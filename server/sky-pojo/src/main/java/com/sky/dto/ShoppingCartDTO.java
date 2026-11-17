package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 购物车加减入参。dishId 与 setmealId 二选一，同时传或都不传都会被业务校验拒绝。
 */
@Data
public class ShoppingCartDTO {

    private Long dishId;

    private Long setmealId;

    @Size(max = 64, message = "口味不能超过 64 个字符")
    private String dishFlavor;
}
