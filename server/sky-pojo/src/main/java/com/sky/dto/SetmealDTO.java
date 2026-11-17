package com.sky.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 套餐新增 / 修改入参。
 * 修改时携带 id，且 setmealDishes 为整体替换语义。
 */
@Data
public class SetmealDTO {

    private Long id;

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64, message = "套餐名称不能超过 64 个字符")
    private String name;

    @NotNull(message = "套餐分类不能为空")
    private Long categoryId;

    @NotNull(message = "套餐价格不能为空")
    @Min(value = 1, message = "套餐价格必须大于 0（单位：分）")
    private Long price;

    @Size(max = 255, message = "图片地址不能超过 255 个字符")
    private String image;

    @Size(max = 255, message = "套餐描述不能超过 255 个字符")
    private String description;

    /** 1 起售，0 停售；新增不传时按停售处理 */
    private Integer status;

    @NotEmpty(message = "套餐至少需要包含一个菜品")
    @Valid
    private List<SetmealDishDTO> setmealDishes;
}
