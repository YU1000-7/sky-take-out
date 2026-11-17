package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.common.context.BaseContext;
import com.sky.common.exception.BusinessException;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.enums.SetmealStatus;
import com.sky.mapper.GoodsQueryMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.GoodsSnapshotVO;
import com.sky.vo.ShoppingCartVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    /** 单个商品在购物车中的数量上限，避免误操作把数量刷到异常值 */
    private static final int MAX_NUMBER = 99;

    private static final Integer GOODS_ON_SALE = 1;

    private final ShoppingCartMapper shoppingCartMapper;
    private final GoodsQueryMapper goodsQueryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(ShoppingCartDTO dto) {
        Long userId = requireUserId();
        normalizeAndValidate(dto);

        ShoppingCart existing = findMatched(userId, dto);
        if (existing != null) {
            int number = existing.getNumber() == null ? 0 : existing.getNumber();
            if (number >= MAX_NUMBER) {
                throw new BusinessException("单个商品最多可加购 " + MAX_NUMBER + " 份");
            }
            existing.setNumber(number + 1);
            shoppingCartMapper.updateById(existing);
            return;
        }

        shoppingCartMapper.insert(buildCart(userId, dto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sub(ShoppingCartDTO dto) {
        Long userId = requireUserId();
        normalizeAndValidate(dto);

        ShoppingCart existing = findMatched(userId, dto);
        if (existing == null) {
            throw new BusinessException("购物车中没有该商品");
        }

        int number = existing.getNumber() == null ? 0 : existing.getNumber();
        if (number <= 1) {
            shoppingCartMapper.deleteById(existing.getId());
            return;
        }
        existing.setNumber(number - 1);
        shoppingCartMapper.updateById(existing);
    }

    @Override
    public List<ShoppingCartVO> list() {
        Long userId = requireUserId();
        List<ShoppingCart> carts = shoppingCartMapper.selectList(Wrappers.<ShoppingCart>lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .orderByAsc(ShoppingCart::getId));
        return carts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clean() {
        Long userId = requireUserId();
        shoppingCartMapper.delete(Wrappers.<ShoppingCart>lambdaQuery()
                .eq(ShoppingCart::getUserId, userId));
    }

    private Long requireUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已失效，请重新登录");
        }
        return userId;
    }

    /**
     * dishId 与 setmealId 必须二选一；空口味统一存为 null，保证去重口径一致。
     */
    private void normalizeAndValidate(ShoppingCartDTO dto) {
        boolean hasDish = dto.getDishId() != null;
        boolean hasSetmeal = dto.getSetmealId() != null;
        if (hasDish == hasSetmeal) {
            throw new BusinessException("菜品和套餐只能选择其中一个");
        }
        if (dto.getDishFlavor() != null) {
            String flavor = dto.getDishFlavor().trim();
            dto.setDishFlavor(flavor.isEmpty() ? null : flavor);
        }
    }

    private ShoppingCart findMatched(Long userId, ShoppingCartDTO dto) {
        LambdaQueryWrapper<ShoppingCart> wrapper = Wrappers.<ShoppingCart>lambdaQuery()
                .eq(ShoppingCart::getUserId, userId);

        if (dto.getDishId() != null) {
            wrapper.eq(ShoppingCart::getDishId, dto.getDishId())
                    .isNull(ShoppingCart::getSetmealId);
            if (dto.getDishFlavor() == null) {
                wrapper.isNull(ShoppingCart::getDishFlavor);
            } else {
                wrapper.eq(ShoppingCart::getDishFlavor, dto.getDishFlavor());
            }
        } else {
            wrapper.eq(ShoppingCart::getSetmealId, dto.getSetmealId())
                    .isNull(ShoppingCart::getDishId);
        }

        List<ShoppingCart> matched = shoppingCartMapper.selectList(wrapper.orderByAsc(ShoppingCart::getId));
        return matched.isEmpty() ? null : matched.get(0);
    }

    /**
     * 加购时写入名称、图片和单价快照，后续菜品或套餐改价不影响购物车已有记录。
     */
    private ShoppingCart buildCart(Long userId, ShoppingCartDTO dto) {
        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setNumber(1);

        if (dto.getDishId() != null) {
            GoodsSnapshotVO dish = goodsQueryMapper.findDish(dto.getDishId());
            if (dish == null) {
                throw new BusinessException("菜品不存在或已被删除");
            }
            if (!GOODS_ON_SALE.equals(dish.getStatus())) {
                throw new BusinessException("菜品「" + dish.getName() + "」已停售");
            }
            cart.setDishId(dish.getId());
            cart.setDishFlavor(dto.getDishFlavor());
            cart.setName(dish.getName());
            cart.setImage(dish.getImage());
            cart.setAmount(dish.getPrice());
            return cart;
        }

        GoodsSnapshotVO setmeal = goodsQueryMapper.findSetmeal(dto.getSetmealId());
        if (setmeal == null) {
            throw new BusinessException("套餐不存在或已被删除");
        }
        if (!SetmealStatus.isOnSale(setmeal.getStatus())) {
            throw new BusinessException("套餐「" + setmeal.getName() + "」已停售");
        }
        cart.setSetmealId(setmeal.getId());
        cart.setName(setmeal.getName());
        cart.setImage(setmeal.getImage());
        cart.setAmount(setmeal.getPrice());
        return cart;
    }

    private ShoppingCartVO toVO(ShoppingCart cart) {
        return ShoppingCartVO.builder()
                .id(cart.getId())
                .dishId(cart.getDishId())
                .setmealId(cart.getSetmealId())
                .name(cart.getName())
                .image(cart.getImage())
                .dishFlavor(cart.getDishFlavor())
                .number(cart.getNumber())
                .amount(cart.getAmount())
                .build();
    }
}
