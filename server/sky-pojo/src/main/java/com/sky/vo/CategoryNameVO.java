package com.sky.vo;

import lombok.Data;

/**
 * 分类名称内部投影，用于套餐列表批量回填分类名称，避免逐条查询。
 */
@Data
public class CategoryNameVO {

    private Long id;
    private String name;
}
