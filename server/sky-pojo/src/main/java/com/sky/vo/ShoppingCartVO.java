package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ShoppingCartVO implements Serializable {

    private Long id;
    private Long userId;
    private Long dishId;
    private Long setmealId;
    private String name;
    private String image;
    private String dishFlavor;
    private Integer number;
    private Long amount;
    private LocalDateTime createTime;
}
