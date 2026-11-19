package com.sky.common.utils;

/**
 * 分页参数统一收敛：页码最小 1，每页条数默认 10、上限 100。
 */
public final class PageParamUtils {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private PageParamUtils() {
    }

    public static Integer normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static Integer normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
