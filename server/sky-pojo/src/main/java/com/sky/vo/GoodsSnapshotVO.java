package com.sky.vo;

import lombok.Data;

/**
 * 商品快照内部投影，仅供购物车、套餐服务读取 dish / setmeal 的只读字段使用，不作为接口出参。
 * 放在 vo 包是为了避免与 B 模块的 Dish 实体、DishVO 重名。
 */
@Data
public class GoodsSnapshotVO {

    private Long id;
    private String name;
    private String image;
    /** 单价，单位分 */
    private Long price;
    /** 1 起售，0 停售 */
    private Integer status;
}
