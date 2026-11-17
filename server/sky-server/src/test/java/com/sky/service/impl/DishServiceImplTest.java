package com.sky.service.impl;

import com.sky.common.exception.BusinessException;
import com.sky.dto.DishDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishServiceImplTest {

    @Mock
    private DishMapper dishMapper;
    @Mock
    private DishFlavorMapper dishFlavorMapper;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private DishServiceImpl dishService;

    @Test
    void shouldRejectStoppingDishReferencedByOnSaleSetmealDuringUpdate() {
        Dish existing = new Dish();
        existing.setId(1L);
        existing.setName("鱼香肉丝");
        existing.setCategoryId(10L);
        existing.setPrice(3800);
        existing.setStatus(1);

        DishDTO dto = new DishDTO();
        dto.setId(1L);
        dto.setName("鱼香肉丝");
        dto.setCategoryId(10L);
        dto.setPrice(3800);
        dto.setStatus(0);

        Category category = new Category();
        category.setId(10L);
        category.setType(1);
        category.setName("川湘菜");

        when(dishMapper.selectById(1L)).thenReturn(existing);
        when(categoryMapper.selectById(10L)).thenReturn(category);
        when(dishMapper.selectCount(any())).thenReturn(0L);
        when(dishMapper.countOnSaleSetmealByDishId(1L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> dishService.update(dto));

        assertEquals("该菜品已被起售中的套餐关联，不能停售", exception.getMessage());
        verify(dishMapper, never()).updateById(any(Dish.class));
        verifyNoInteractions(dishFlavorMapper);
    }
}
