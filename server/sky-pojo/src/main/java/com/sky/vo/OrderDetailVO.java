package com.sky.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderDetailVO implements Serializable {

    private Long id;
    private Long orderId;
    private String name;
    private String image;
    private Long dishId;
    private Long setmealId;
    private String dishFlavor;
    private Integer number;
    private Long amount;
}
