package com.sky.service.impl;

import com.sky.common.context.BaseContext;
import com.sky.common.exception.BusinessException;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.GoodsQueryMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.vo.GoodsSnapshotVO;
import com.sky.vo.ShoppingCartVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceImplTest {

    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private GoodsQueryMapper goodsQueryMapper;

    @InjectMocks
    private ShoppingCartServiceImpl shoppingCartService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(9L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    void shouldInsertCartWithGoodsSnapshot() {
        when(shoppingCartMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(goodsQueryMapper.findDish(1L)).thenReturn(goods(1L, "鱼香肉丝", 3800L, 1));

        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);
        dto.setDishFlavor(" 微辣 ");
        shoppingCartService.add(dto);

        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).insert(captor.capture());
        ShoppingCart saved = captor.getValue();
        assertEquals(9L, saved.getUserId().longValue());
        assertEquals("鱼香肉丝", saved.getName());
        assertEquals(3800L, saved.getAmount().longValue());
        assertEquals("微辣", saved.getDishFlavor(), "口味首尾空格需要被清理");
        assertEquals(1, saved.getNumber().intValue());
    }

    @Test
    void shouldIncreaseNumberWhenSameDishAndFlavorExists() {
        ShoppingCart existing = new ShoppingCart();
        existing.setId(5L);
        existing.setNumber(2);
        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(existing));

        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);
        dto.setDishFlavor("微辣");
        shoppingCartService.add(dto);

        verify(shoppingCartMapper).updateById(existing);
        assertEquals(3, existing.getNumber().intValue());
        verify(shoppingCartMapper, never()).insert(any());
    }

    @Test
    void shouldDeleteRecordWhenReduceFromOne() {
        ShoppingCart existing = new ShoppingCart();
        existing.setId(7L);
        existing.setNumber(1);
        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(existing));

        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setSetmealId(2L);
        shoppingCartService.sub(dto);

        verify(shoppingCartMapper).deleteById(7L);
    }

    @Test
    void shouldRejectStoppedDish() {
        when(shoppingCartMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(goodsQueryMapper.findDish(1L)).thenReturn(goods(1L, "停售小炒", 2800L, 0));

        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> shoppingCartService.add(dto));
        assertEquals("菜品「停售小炒」已停售", exception.getMessage());
        verify(shoppingCartMapper, never()).insert(any());
    }

    @Test
    void shouldRejectWhenDishAndSetmealBothProvided() {
        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);
        dto.setSetmealId(1L);

        assertThrows(BusinessException.class, () -> shoppingCartService.add(dto));
    }

    @Test
    void shouldRejectWhenNotLogin() {
        BaseContext.removeCurrentId();

        ShoppingCartDTO dto = new ShoppingCartDTO();
        dto.setDishId(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> shoppingCartService.add(dto));
        assertEquals(401, exception.getCode());
    }

    @Test
    void shouldMapCartListToVO() {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setDishId(1L);
        cart.setName("鱼香肉丝");
        cart.setNumber(2);
        cart.setAmount(3800L);
        cart.setDishFlavor("微辣");
        when(shoppingCartMapper.selectList(any())).thenReturn(List.of(cart));

        List<ShoppingCartVO> result = shoppingCartService.list();

        assertEquals(1, result.size());
        assertEquals("鱼香肉丝", result.get(0).getName());
        assertEquals(2, result.get(0).getNumber().intValue());
    }

    private GoodsSnapshotVO goods(Long id, String name, Long price, Integer status) {
        GoodsSnapshotVO goods = new GoodsSnapshotVO();
        goods.setId(id);
        goods.setName(name);
        goods.setPrice(price);
        goods.setStatus(status);
        goods.setImage("http://example.com/" + id + ".jpg");
        return goods;
    }
}
