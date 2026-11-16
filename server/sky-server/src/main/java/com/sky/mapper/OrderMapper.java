package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Orders;

/**
 * 订单主表数据访问。分页、按状态筛选和报表区间查询都用 QueryWrapper 完成，
 * 无需额外 XML 或注解 SQL。
 */
public interface OrderMapper extends BaseMapper<Orders> {
}
