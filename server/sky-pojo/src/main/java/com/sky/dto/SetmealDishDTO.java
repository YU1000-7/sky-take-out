package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SetmealDishDTO implements Serializable {

    private Long id;
    private Long setmealId;

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    private String dishName;
    private Long price;

    @NotNull(message = "菜品份数不能为空")
    @Min(value = 1, message = "菜品份数至少为 1")
    private Integer copies;
}
