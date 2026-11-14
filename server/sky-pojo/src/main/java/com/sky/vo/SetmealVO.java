package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SetmealVO implements Serializable {

    private Long id;
    private Long categoryId;
    private String name;
    private Long price;
    private Integer status;
    private String description;
    private String image;
    private LocalDateTime updateTime;
    private List<SetmealDishVO> setmealDishes = new ArrayList<>();
}
