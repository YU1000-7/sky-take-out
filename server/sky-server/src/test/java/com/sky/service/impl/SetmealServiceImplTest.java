package com.sky.service.impl;

import com.sky.common.exception.BusinessException;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealDishDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.GoodsQueryMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.vo.GoodsSnapshotVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetmealServiceImplTest {

    @Mock
    private SetmealMapper setmealMapper;
    @Mock
    private SetmealDishMapper setmealDishMapper;
    @Mock
    private GoodsQueryMapper goodsQueryMapper;

    @InjectMocks
    private SetmealServiceImpl setmealService;

    @Test
    void shouldSaveSetmealWithDishSnapshot() {
        when(goodsQueryMapper.findCategoryType(10L)).thenReturn(2);
        when(goodsQueryMapper.findDishes(anyCollection())).thenReturn(List.of(
                goods(1L, "鱼香肉丝", 3800L), goods(2L, "宫保鸡丁", 4200L)));
        when(setmealMapper.insert(any(Setmeal.class))).thenAnswer(invocation -> {
            Setmeal setmeal = invocation.getArgument(0);
            setmeal.setId(100L);
            return 1;
        });

        setmealService.save(dto(0));

        ArgumentCaptor<Setmeal> setmealCaptor = ArgumentCaptor.forClass(Setmeal.class);
        verify(setmealMapper).insert(setmealCaptor.capture());
        assertEquals(0, setmealCaptor.getValue().getStatus().intValue(), "新增默认落为停售");

        ArgumentCaptor<SetmealDish> dishCaptor = ArgumentCaptor.forClass(SetmealDish.class);
        verify(setmealDishMapper, times(2)).insert(dishCaptor.capture());
        SetmealDish first = dishCaptor.getAllValues().get(0);
        assertEquals(100L, first.getSetmealId().longValue());
        assertEquals("鱼香肉丝", first.getName(), "关联表需要写入菜品名称快照");
        assertEquals(3800L, first.getPrice().longValue(), "关联表需要写入菜品价格快照");
    }

    @Test
    void shouldRejectCategoryThatIsNotSetmealType() {
        when(goodsQueryMapper.findCategoryType(10L)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> setmealService.save(dto(0)));
        assertEquals("所选分类不是套餐分类，请选择类型为套餐的分类", exception.getMessage());
        verify(setmealMapper, never()).insert(any(Setmeal.class));
    }

    @Test
    void shouldRejectDuplicateDish() {
        when(goodsQueryMapper.findCategoryType(10L)).thenReturn(2);

        SetmealDTO dto = dto(0);
        dto.getSetmealDishes().add(dto.getSetmealDishes().get(0));

        BusinessException exception = assertThrows(BusinessException.class, () -> setmealService.save(dto));
        assertEquals("套餐内菜品重复，请合并相同菜品的份数", exception.getMessage());
    }

    @Test
    void shouldRejectOnSaleWhenDishUnavailable() {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setStatus(0);
        when(setmealMapper.selectById(1L)).thenReturn(setmeal);
        when(setmealDishMapper.selectCount(any())).thenReturn(2L);
        when(setmealDishMapper.selectUnavailableDishNames(1L)).thenReturn(List.of("停售小炒"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> setmealService.startOrStop(1, 1L));
        assertEquals("套餐内含停售或已删除菜品，无法起售：停售小炒", exception.getMessage());
        verify(setmealMapper, never()).updateById(any(Setmeal.class));
    }

    @Test
    void shouldRejectOnSaleWhenNoDish() {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setStatus(0);
        when(setmealMapper.selectById(1L)).thenReturn(setmeal);
        when(setmealDishMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> setmealService.startOrStop(1, 1L));
        assertEquals("套餐内还没有菜品，无法起售", exception.getMessage());
    }

    @Test
    void shouldRejectDeleteWhenSetmealOnSale() {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setName("双人特惠餐");
        setmeal.setStatus(1);
        when(setmealMapper.selectBatchIds(anyCollection())).thenReturn(List.of(setmeal));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> setmealService.delete("1"));
        assertEquals("套餐正在起售中，请先停售再删除：双人特惠餐", exception.getMessage());
        verify(setmealMapper, never()).deleteBatchIds(anyCollection());
    }

    @Test
    void shouldRejectDeleteWhenReferencedByCart() {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setName("双人特惠餐");
        setmeal.setStatus(0);
        when(setmealMapper.selectBatchIds(anyCollection())).thenReturn(List.of(setmeal));
        when(goodsQueryMapper.countCartBySetmealIds(anyCollection())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> setmealService.delete("1"));
        assertEquals("套餐已被客户加入购物车，无法删除，建议改为停售", exception.getMessage());
    }

    @Test
    void shouldRejectDeleteWhenReferencedByActiveOrder() {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setName("双人特惠餐");
        setmeal.setStatus(0);
        when(setmealMapper.selectBatchIds(anyCollection())).thenReturn(List.of(setmeal));
        when(goodsQueryMapper.countCartBySetmealIds(anyCollection())).thenReturn(0L);
        when(goodsQueryMapper.countActiveOrderBySetmealIds(anyCollection())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> setmealService.delete("1"));
        assertEquals("套餐已被有效订单引用，无法删除，建议改为停售", exception.getMessage());
    }

    @Test
    void shouldReplaceSetmealDishesOnUpdate() {
        Setmeal existing = new Setmeal();
        existing.setId(1L);
        existing.setStatus(1);
        when(setmealMapper.selectById(1L)).thenReturn(existing);
        when(goodsQueryMapper.findCategoryType(10L)).thenReturn(2);
        when(goodsQueryMapper.findDishes(anyCollection())).thenReturn(List.of(goods(1L, "鱼香肉丝", 3800L)));
        when(setmealDishMapper.selectCount(any())).thenReturn(1L);
        when(setmealDishMapper.selectUnavailableDishNames(1L)).thenReturn(List.of());

        SetmealDTO dto = dto(1);
        dto.setId(1L);
        dto.getSetmealDishes().remove(1);

        setmealService.update(dto);

        verify(setmealDishMapper).delete(any());
        verify(setmealDishMapper).insert(any(SetmealDish.class));
        verify(setmealMapper).updateById(existing);
    }

    private SetmealDTO dto(int status) {
        SetmealDishDTO first = new SetmealDishDTO();
        first.setDishId(1L);
        first.setCopies(1);
        SetmealDishDTO second = new SetmealDishDTO();
        second.setDishId(2L);
        second.setCopies(2);

        SetmealDTO dto = new SetmealDTO();
        dto.setName("双人特惠餐");
        dto.setCategoryId(10L);
        dto.setPrice(6800L);
        dto.setStatus(status);
        dto.setSetmealDishes(new java.util.ArrayList<>(List.of(first, second)));
        return dto;
    }

    private GoodsSnapshotVO goods(Long id, String name, Long price) {
        GoodsSnapshotVO goods = new GoodsSnapshotVO();
        goods.setId(id);
        goods.setName(name);
        goods.setPrice(price);
        goods.setStatus(1);
        return goods;
    }
}
