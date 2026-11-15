package com.sky.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 套餐出参。分页查询不返回 setmealDishes，详情查询会带出套餐内菜品。
 */
@Data
@Builder
public class SetmealVO {

    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long price;
    private String image;
    private String description;
    private Integer status;
    private LocalDateTime updateTime;
    private List<SetmealDishVO> setmealDishes;
}
