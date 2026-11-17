package com.sky.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 套餐实体，对应 setmeal 表。
 */
@Data
@TableName("setmeal")
public class Setmeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套餐分类 ID，对应 category 表中 type=2 的记录 */
    private Long categoryId;

    private String name;

    /** 套餐价格，单位分 */
    private Long price;

    /** 1 起售，0 停售 */
    private Integer status;

    private String description;

    private String image;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
