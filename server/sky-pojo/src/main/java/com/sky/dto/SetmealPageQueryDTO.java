package com.sky.dto;

import lombok.Data;

/**
 * 套餐分页查询入参。
 */
@Data
public class SetmealPageQueryDTO {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String name;
    private Long categoryId;
    private Integer status;
}
