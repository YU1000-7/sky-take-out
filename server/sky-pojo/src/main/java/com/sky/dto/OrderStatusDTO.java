package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class OrderStatusDTO implements Serializable {

    @NotNull(message = "订单 ID 不能为空")
    private Long id;

    private Integer status;
}
