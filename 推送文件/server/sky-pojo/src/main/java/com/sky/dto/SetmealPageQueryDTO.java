package com.sky.dto;

import lombok.Data;

/**
 * 套餐分页查询入参。
 */
@Data
public class SetmealPageQueryDTO {

    private Integer page = 1;
    private Integer pageSize = 10;

    public void setPage(Integer page) {
        this.page = com.sky.common.utils.PageParamUtils.normalizePage(page);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = com.sky.common.utils.PageParamUtils.normalizePageSize(pageSize);
    }
    private String name;
    private Long categoryId;
    private Integer status;
}
