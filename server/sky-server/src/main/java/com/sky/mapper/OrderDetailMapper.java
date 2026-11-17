package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.OrderDetail;

/**
 * 订单明细数据访问。明细同时兼容菜品（dishId）和套餐（setmealId）。
 */
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
}
