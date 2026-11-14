package com.sky.common.enums;

import lombok.Getter;

@Getter
public enum DishStatus {

    DISCONTINUED(0, "停售"),
    ON_SALE(1, "起售");

    private final int code;
    private final String description;

    DishStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
