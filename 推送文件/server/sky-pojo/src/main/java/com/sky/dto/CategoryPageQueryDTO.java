package com.sky.dto;

import lombok.Data;

@Data
public class CategoryPageQueryDTO {

    private Integer page = 1;
    private Integer pageSize = 10;
    private String name;
    private Integer type;

    public void setPage(Integer page) {
        this.page = com.sky.common.utils.PageParamUtils.normalizePage(page);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = com.sky.common.utils.PageParamUtils.normalizePageSize(pageSize);
    }
}
