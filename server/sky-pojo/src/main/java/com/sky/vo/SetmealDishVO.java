package com.sky.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 套餐内菜品出参，用于 B 端套餐详情回显。
 * dishName、price、image 为保存时快照，同时返回菜品当前是否停售便于后台发现问题。
 */
@Data
@Builder
public class SetmealDishVO {

    private Long dishId;
    private String dishName;
    private Integer copies;
    private Long price;
    private String image;
    /** 菜品当前状态：1 起售，0 停售；菜品已被删除时为 null */
    private Integer dishStatus;
}
