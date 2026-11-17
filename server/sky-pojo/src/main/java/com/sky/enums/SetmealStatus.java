package com.sky.enums;

/**
 * 套餐状态。数据库使用 Integer 存储，禁止在业务代码里散落魔法数字。
 */
public enum SetmealStatus {

    SOLD_OUT(0, "停售"),
    ON_SALE(1, "起售");

    private final Integer code;
    private final String desc;

    SetmealStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static boolean isValid(Integer code) {
        return ON_SALE.code.equals(code) || SOLD_OUT.code.equals(code);
    }

    public static boolean isOnSale(Integer code) {
        return ON_SALE.code.equals(code);
    }

    public static String descOf(Integer code) {
        for (SetmealStatus status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知";
    }
}
