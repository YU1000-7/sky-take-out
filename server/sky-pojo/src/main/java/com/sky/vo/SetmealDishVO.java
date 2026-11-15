package com.sky.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SetmealDishVO implements Serializable {

    private Long id;
    private Long dishId;
    private String dishName;
    private Long price;
    private Integer copies;
}
