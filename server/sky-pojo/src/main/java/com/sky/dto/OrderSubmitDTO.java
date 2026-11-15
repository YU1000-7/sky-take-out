package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class OrderSubmitDTO implements Serializable {

    @NotNull(message = "收货地址不能为空")
    private Long addressBookId;

    @NotNull(message = "支付方式不能为空")
    private Integer payMethod;

    private String remark;
}
