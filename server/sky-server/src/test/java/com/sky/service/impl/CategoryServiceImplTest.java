package com.sky.service.impl;

import com.sky.common.exception.BusinessException;
import com.sky.dto.CategoryDTO;
import com.sky.entity.Category;
import com.sky.mapper.CategoryMapper;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void shouldRejectChangingCategoryType() {
        Category existing = new Category();
        existing.setId(1L);
        existing.setType(1);
        existing.setName("热菜");
        existing.setSort(1);

        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setType(2);
        dto.setName("热菜");
        dto.setSort(1);

        when(categoryMapper.selectById(1L)).thenReturn(existing);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.update(dto));

        assertEquals("分类类型不允许修改", exception.getMessage());
        verify(categoryMapper, never()).updateById(any(Category.class));
    }
}
