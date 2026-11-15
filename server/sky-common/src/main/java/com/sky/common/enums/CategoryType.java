package com.sky.common.enums;

import lombok.Getter;

@Getter
public enum CategoryType {

    DISH(1, "菜品分类"),
    SETMEAL(2, "套餐分类");

    private final int code;
    private final String description;

    CategoryType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
