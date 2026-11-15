package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ShoppingCartPageQueryDTO implements Serializable {

    private Integer page;
    private Integer pageSize;
}
