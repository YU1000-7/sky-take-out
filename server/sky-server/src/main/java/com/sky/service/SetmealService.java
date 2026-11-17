package com.sky.service;

import com.sky.common.result.PageResult;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.vo.SetmealDishDetailVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    PageResult<SetmealVO> page(SetmealPageQueryDTO dto);

    SetmealVO getById(Long id);

    void save(SetmealDTO dto);

    void update(SetmealDTO dto);

    void delete(String ids);

    void startOrStop(Integer status, Long id);

    /**
     * C 端：按分类查询起售套餐，categoryId 为空时返回全部起售套餐。
     */
    List<SetmealVO> listOnSaleByCategory(Long categoryId);

    /**
     * C 端：查询套餐内起售菜品。
     */
    List<SetmealDishDetailVO> listDishBySetmealId(Long id);
}
