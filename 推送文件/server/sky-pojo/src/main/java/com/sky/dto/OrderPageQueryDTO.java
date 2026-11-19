package com.sky.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrderPageQueryDTO implements Serializable {

    private Integer page;
    private Integer pageSize;

    public void setPage(Integer page) {
        this.page = com.sky.common.utils.PageParamUtils.normalizePage(page);
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = com.sky.common.utils.PageParamUtils.normalizePageSize(pageSize);
    }
    private String number;
    private Integer status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
