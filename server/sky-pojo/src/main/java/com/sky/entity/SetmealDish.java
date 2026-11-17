package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 套餐与菜品关联实体，对应 setmeal_dish 表。
 * name、price 为保存时的菜品快照，避免菜品后续改动影响已配置的套餐。
 */
@Data
@TableName("setmeal_dish")
public class SetmealDish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long setmealId;

    private Long dishId;

    /** 菜品名称快照 */
    private String name;

    /** 菜品价格快照，单位分 */
    private Long price;

    /** 份数 */
    private Integer copies;
}
