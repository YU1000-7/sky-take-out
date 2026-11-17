package com.sky.mapper;

import com.sky.vo.CategoryNameVO;
import com.sky.vo.GoodsSnapshotVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 跨模块只读查询适配层。
 *
 * <p>套餐与购物车需要读取 dish、category、order_detail 等由 B / D 模块维护的表。
 * 为了不依赖对方尚未提交的实体和 Mapper，这里集中存放只读 SQL；
 * 待 B 的 DishMapper、CategoryMapper 稳定后可替换为对其服务的调用，接口签名保持不变。</p>
 */
public interface GoodsQueryMapper {

    @Select("SELECT id, name, image, price, status FROM dish WHERE id = #{id}")
    GoodsSnapshotVO findDish(@Param("id") Long id);

    @Select("SELECT id, name, image, price, status FROM setmeal WHERE id = #{id}")
    GoodsSnapshotVO findSetmeal(@Param("id") Long id);

    @Select("<script>SELECT id, name, image, price, status FROM dish WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<GoodsSnapshotVO> findDishes(@Param("ids") Collection<Long> ids);

    @Select("SELECT type FROM category WHERE id = #{id}")
    Integer findCategoryType(@Param("id") Long id);

    @Select("SELECT name FROM category WHERE id = #{id}")
    String findCategoryName(@Param("id") Long id);

    @Select("<script>SELECT id, name FROM category WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<CategoryNameVO> findCategoryNames(@Param("ids") Collection<Long> ids);

    /**
     * 统计被购物车引用的套餐数量，用于删除前拦截。
     */
    @Select("<script>SELECT COUNT(1) FROM shopping_cart WHERE setmeal_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    long countCartBySetmealIds(@Param("ids") Collection<Long> ids);

    /**
     * 统计被有效订单（待接单、已接单、派送中）引用的套餐数量，用于删除前拦截。
     */
    @Select("<script>SELECT COUNT(1) FROM order_detail od INNER JOIN orders o ON o.id = od.order_id"
            + " WHERE od.setmeal_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + " AND o.status IN (1, 2, 3)"
            + "</script>")
    long countActiveOrderBySetmealIds(@Param("ids") Collection<Long> ids);
}
