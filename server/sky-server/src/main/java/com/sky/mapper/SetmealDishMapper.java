package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.SetmealDish;
import com.sky.vo.SetmealDishDetailVO;
import com.sky.vo.SetmealDishVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 套餐菜品关联 Mapper。
 * 关联查询统一走 dish 表取最新名称、价格和图片，dish 已被删除时退回 setmeal_dish 中的快照。
 */
public interface SetmealDishMapper extends BaseMapper<SetmealDish> {

    /**
     * B 端套餐详情使用：带菜品当前状态，便于后台发现套餐内有停售或已删除菜品。
     */
    @Select("SELECT sd.dish_id AS dishId,"
            + " COALESCE(d.name, sd.name) AS dishName,"
            + " sd.copies AS copies,"
            + " COALESCE(d.price, sd.price) AS price,"
            + " COALESCE(d.image, '') AS image,"
            + " d.status AS dishStatus"
            + " FROM setmeal_dish sd LEFT JOIN dish d ON d.id = sd.dish_id"
            + " WHERE sd.setmeal_id = #{setmealId} ORDER BY sd.id")
    List<SetmealDishVO> selectSetmealDishes(@Param("setmealId") Long setmealId);

    /**
     * C 端套餐内菜品使用：只返回仍然存在且起售的菜品。
     */
    @Select("SELECT d.id AS id, d.name AS name, d.image AS image, d.price AS price, sd.copies AS copies"
            + " FROM setmeal_dish sd INNER JOIN dish d ON d.id = sd.dish_id"
            + " WHERE sd.setmeal_id = #{setmealId} AND d.status = 1 ORDER BY sd.id")
    List<SetmealDishDetailVO> selectSetmealDishDetails(@Param("setmealId") Long setmealId);

    /**
     * 套餐起售校验：返回套餐内处于停售状态或已被删除的菜品名称。
     */
    @Select("SELECT COALESCE(d.name, sd.name) FROM setmeal_dish sd LEFT JOIN dish d ON d.id = sd.dish_id"
            + " WHERE sd.setmeal_id = #{setmealId} AND (d.id IS NULL OR d.status <> 1)")
    List<String> selectUnavailableDishNames(@Param("setmealId") Long setmealId);
}
