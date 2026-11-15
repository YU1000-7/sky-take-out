package com.sky.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SetmealDTO implements Serializable {

    private Long id;

    @NotNull(message = "套餐分类不能为空")
    private Long categoryId;

    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @NotNull(message = "套餐价格不能为空")
    private Long price;

    private Integer status;
    private String description;
    private String image;

    @Valid
    @NotEmpty(message = "套餐至少包含一个菜品")
    private List<SetmealDishDTO> setmealDishes = new ArrayList<>();
}
