package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 套餐内菜品入参。菜品名称和价格由后端按 dish 表回填快照，前端无需传。
 */
@Data
public class SetmealDishDTO {

    @NotNull(message = "套餐内菜品 ID 不能为空")
    private Long dishId;

    @NotNull(message = "套餐内菜品份数不能为空")
    @Min(value = 1, message = "套餐内菜品份数至少为 1")
    @Max(value = 99, message = "套餐内菜品份数不能超过 99")
    private Integer copies;
}
