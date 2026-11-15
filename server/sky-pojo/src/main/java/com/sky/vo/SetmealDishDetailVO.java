package com.sky.vo;

import lombok.Builder;
import lombok.Data;

/**
 * C 端套餐内菜品出参。字段与接口文档 /user/setmeal/dish/{id} 的 id、name、image、price 一致。
 */
@Data
@Builder
public class SetmealDishDetailVO {

    private Long id;
    private String name;
    private String image;
    private Long price;
    /** 套餐内份数 */
    private Integer copies;
}
