package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AddressBookPageQueryDTO implements Serializable {

    private Integer page;
    private Integer pageSize;

    public void setPage(Integer page) {
        this.page = com.sky.common.utils.PageParamUtils.normalizePage(page);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = com.sky.common.utils.PageParamUtils.normalizePageSize(pageSize);
    }
}
