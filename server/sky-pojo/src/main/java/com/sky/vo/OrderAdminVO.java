package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderAdminVO implements Serializable {

    private OrderVO order;
    private List<OrderDetailVO> orderDetailList = new ArrayList<>();
}
