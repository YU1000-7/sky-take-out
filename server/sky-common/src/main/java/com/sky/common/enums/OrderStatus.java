package com.sky.common.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING(1, "待接单"),
    CONFIRMED(2, "已接单"),
    DELIVERING(3, "派送中"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
