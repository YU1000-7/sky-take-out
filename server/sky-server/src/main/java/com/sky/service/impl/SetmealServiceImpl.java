package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.common.exception.BusinessException;
import com.sky.common.result.PageResult;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealDishDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enums.SetmealStatus;
import com.sky.mapper.GoodsQueryMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import com.sky.vo.CategoryNameVO;
import com.sky.vo.GoodsSnapshotVO;
import com.sky.vo.SetmealDishDetailVO;
import com.sky.vo.SetmealDishVO;
import com.sky.vo.SetmealVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SetmealServiceImpl implements SetmealService {

    /** 套餐分类在 category 表中的 type 值 */
    private static final Integer CATEGORY_TYPE_SETMEAL = 2;

    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;
    private final GoodsQueryMapper goodsQueryMapper;

    @Override
    public PageResult<SetmealVO> page(SetmealPageQueryDTO dto) {
        int pageNumber = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        pageSize = Math.min(pageSize, 100);

        Page<Setmeal> page = setmealMapper.selectPage(
                new Page<>(pageNumber, pageSize),
                Wrappers.<Setmeal>lambdaQuery()
                        .like(StringUtils.hasText(dto.getName()), Setmeal::getName, dto.getName())
                        .eq(dto.getCategoryId() != null, Setmeal::getCategoryId, dto.getCategoryId())
                        .eq(dto.getStatus() != null, Setmeal::getStatus, dto.getStatus())
                        .orderByDesc(Setmeal::getUpdateTime, Setmeal::getId)
        );

        Map<Long, String> categoryNames = loadCategoryNames(page.getRecords());
        List<SetmealVO> records = page.getRecords().stream()
                .map(setmeal -> toVO(setmeal, categoryNames, null))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = requireSetmeal(id);
        Map<Long, String> categoryNames = loadCategoryNames(List.of(setmeal));
        return toVO(setmeal, categoryNames, setmealDishMapper.selectSetmealDishes(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SetmealDTO dto) {
        normalize(dto);
        ensureCategoryValid(dto.getCategoryId());
        List<SetmealDish> setmealDishes = buildSetmealDishes(dto.getSetmealDishes());

        Integer status = dto.getStatus() == null ? SetmealStatus.SOLD_OUT.getCode() : dto.getStatus();
        ensureStatusValid(status);

        Setmeal setmeal = new Setmeal();
        fillSetmeal(setmeal, dto);
        // 先落库为停售，再按起售规则校验后改为起售，避免绕过“套餐内菜品必须起售”的约束。
        setmeal.setStatus(SetmealStatus.SOLD_OUT.getCode());
        setmealMapper.insert(setmeal);
        saveSetmealDishes(setmeal.getId(), setmealDishes);

        if (SetmealStatus.isOnSale(status)) {
            ensureDishesAvailable(setmeal.getId());
            setmeal.setStatus(SetmealStatus.ON_SALE.getCode());
            setmealMapper.updateById(setmeal);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SetmealDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("套餐 ID 不能为空");
        }
        Setmeal existing = requireSetmeal(dto.getId());

        normalize(dto);
        ensureCategoryValid(dto.getCategoryId());
        List<SetmealDish> setmealDishes = buildSetmealDishes(dto.getSetmealDishes());

        Integer status = dto.getStatus() == null ? existing.getStatus() : dto.getStatus();
        ensureStatusValid(status);

        fillSetmeal(existing, dto);
        existing.setStatus(status);
        setmealMapper.updateById(existing);

        // 套餐与菜品关联整体替换，避免残留已被移除的菜品。
        removeSetmealDishes(dto.getId());
        saveSetmealDishes(dto.getId(), setmealDishes);

        if (SetmealStatus.isOnSale(status)) {
            ensureDishesAvailable(dto.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String ids) {
        List<Long> idList = parseIds(ids);
        List<Setmeal> setmeals = setmealMapper.selectBatchIds(idList);
        if (setmeals.size() != idList.size()) {
            throw new BusinessException("部分套餐不存在或已被删除，请刷新后重试");
        }

        List<String> onSaleNames = setmeals.stream()
                .filter(setmeal -> SetmealStatus.isOnSale(setmeal.getStatus()))
                .map(Setmeal::getName)
                .collect(Collectors.toList());
        if (!onSaleNames.isEmpty()) {
            throw new BusinessException("套餐正在起售中，请先停售再删除：" + String.join("、", onSaleNames));
        }

        if (goodsQueryMapper.countCartBySetmealIds(idList) > 0) {
            throw new BusinessException("套餐已被客户加入购物车，无法删除，建议改为停售");
        }
        if (goodsQueryMapper.countActiveOrderBySetmealIds(idList) > 0) {
            throw new BusinessException("套餐已被有效订单引用，无法删除，建议改为停售");
        }

        setmealMapper.deleteBatchIds(idList);
        for (Long id : idList) {
            removeSetmealDishes(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startOrStop(Integer status, Long id) {
        ensureStatusValid(status);
        if (id == null) {
            throw new BusinessException("套餐 ID 不能为空");
        }
        Setmeal setmeal = requireSetmeal(id);

        if (SetmealStatus.isOnSale(status)) {
            ensureDishesAvailable(id);
        }

        setmeal.setStatus(status);
        setmealMapper.updateById(setmeal);
    }

    @Override
    public List<SetmealVO> listOnSaleByCategory(Long categoryId) {
        List<Setmeal> setmeals = setmealMapper.selectList(Wrappers.<Setmeal>lambdaQuery()
                .eq(Setmeal::getStatus, SetmealStatus.ON_SALE.getCode())
                .eq(categoryId != null, Setmeal::getCategoryId, categoryId)
                .orderByAsc(Setmeal::getCategoryId)
                .orderByDesc(Setmeal::getId));

        Map<Long, String> categoryNames = loadCategoryNames(setmeals);
        return setmeals.stream()
                .map(setmeal -> toVO(setmeal, categoryNames, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<SetmealDishDetailVO> listDishBySetmealId(Long id) {
        Setmeal setmeal = requireSetmeal(id);
        if (!SetmealStatus.isOnSale(setmeal.getStatus())) {
            throw new BusinessException("套餐已停售");
        }
        return setmealDishMapper.selectSetmealDishDetails(id);
    }

    private Setmeal requireSetmeal(Long id) {
        if (id == null) {
            throw new BusinessException("套餐 ID 不能为空");
        }
        Setmeal setmeal = setmealMapper.selectById(id);
        if (setmeal == null) {
            throw new BusinessException("套餐不存在");
        }
        return setmeal;
    }

    private void ensureStatusValid(Integer status) {
        if (!SetmealStatus.isValid(status)) {
            throw new BusinessException("套餐状态只能是 0（停售）或 1（起售）");
        }
    }

    private void ensureCategoryValid(Long categoryId) {
        Integer type = goodsQueryMapper.findCategoryType(categoryId);
        if (type == null) {
            throw new BusinessException("套餐分类不存在，请先在分类管理中创建套餐分类");
        }
        if (!CATEGORY_TYPE_SETMEAL.equals(type)) {
            throw new BusinessException("所选分类不是套餐分类，请选择类型为套餐的分类");
        }
    }

    /**
     * 校验套餐内菜品是否存在、是否重复，并生成带名称和价格快照的关联记录。
     */
    private List<SetmealDish> buildSetmealDishes(List<SetmealDishDTO> dishDTOS) {
        if (dishDTOS == null || dishDTOS.isEmpty()) {
            throw new BusinessException("套餐至少需要包含一个菜品");
        }

        Set<Long> dishIds = new LinkedHashSet<>();
        for (SetmealDishDTO item : dishDTOS) {
            if (item.getDishId() == null) {
                throw new BusinessException("套餐内菜品 ID 不能为空");
            }
            if (!dishIds.add(item.getDishId())) {
                throw new BusinessException("套餐内菜品重复，请合并相同菜品的份数");
            }
        }

        Map<Long, GoodsSnapshotVO> dishMap = goodsQueryMapper.findDishes(dishIds).stream()
                .collect(Collectors.toMap(GoodsSnapshotVO::getId, dish -> dish));

        List<Long> missing = dishIds.stream()
                .filter(dishId -> !dishMap.containsKey(dishId))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new BusinessException("以下菜品不存在：" + missing.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("、")));
        }

        List<SetmealDish> result = new ArrayList<>(dishDTOS.size());
        for (SetmealDishDTO item : dishDTOS) {
            GoodsSnapshotVO dish = dishMap.get(item.getDishId());
            SetmealDish setmealDish = new SetmealDish();
            setmealDish.setDishId(dish.getId());
            setmealDish.setCopies(item.getCopies() == null ? 1 : item.getCopies());
            setmealDish.setName(dish.getName());
            setmealDish.setPrice(dish.getPrice());
            result.add(setmealDish);
        }
        return result;
    }

    private void saveSetmealDishes(Long setmealId, List<SetmealDish> setmealDishes) {
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
            setmealDishMapper.insert(setmealDish);
        }
    }

    private void removeSetmealDishes(Long setmealId) {
        setmealDishMapper.delete(Wrappers.<SetmealDish>lambdaQuery()
                .eq(SetmealDish::getSetmealId, setmealId));
    }

    /**
     * 起售校验：套餐内必须有菜品，且菜品全部处于起售状态。
     */
    private void ensureDishesAvailable(Long setmealId) {
        Long dishCount = setmealDishMapper.selectCount(Wrappers.<SetmealDish>lambdaQuery()
                .eq(SetmealDish::getSetmealId, setmealId));
        if (dishCount == null || dishCount == 0) {
            throw new BusinessException("套餐内还没有菜品，无法起售");
        }

        List<String> unavailable = setmealDishMapper.selectUnavailableDishNames(setmealId);
        if (!unavailable.isEmpty()) {
            throw new BusinessException("套餐内含停售或已删除菜品，无法起售：" + String.join("、", unavailable));
        }
    }

    private void normalize(SetmealDTO dto) {
        dto.setName(dto.getName().trim());
        if (dto.getImage() != null) {
            dto.setImage(dto.getImage().trim());
        }
        if (dto.getDescription() != null) {
            dto.setDescription(dto.getDescription().trim());
        }
    }

    private void fillSetmeal(Setmeal setmeal, SetmealDTO dto) {
        setmeal.setName(dto.getName());
        setmeal.setCategoryId(dto.getCategoryId());
        setmeal.setPrice(dto.getPrice());
        setmeal.setImage(dto.getImage());
        setmeal.setDescription(dto.getDescription());
    }

    private Map<Long, String> loadCategoryNames(List<Setmeal> setmeals) {
        Set<Long> categoryIds = setmeals.stream()
                .map(Setmeal::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        for (CategoryNameVO category : goodsQueryMapper.findCategoryNames(categoryIds)) {
            result.put(category.getId(), category.getName());
        }
        return result;
    }

    private SetmealVO toVO(Setmeal setmeal, Map<Long, String> categoryNames, List<SetmealDishVO> setmealDishes) {
        return SetmealVO.builder()
                .id(setmeal.getId())
                .name(setmeal.getName())
                .categoryId(setmeal.getCategoryId())
                .categoryName(categoryNames.get(setmeal.getCategoryId()))
                .price(setmeal.getPrice())
                .image(setmeal.getImage())
                .description(setmeal.getDescription())
                .status(setmeal.getStatus())
                .updateTime(setmeal.getUpdateTime())
                .setmealDishes(setmealDishes)
                .build();
    }

    private List<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            throw new BusinessException("请选择要删除的套餐");
        }
        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            String value = part.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                result.add(Long.valueOf(value));
            } catch (NumberFormatException exception) {
                throw new BusinessException("套餐 ID 格式不正确：" + value);
            }
        }
        if (result.isEmpty()) {
            throw new BusinessException("请选择要删除的套餐");
        }
        return result;
    }
}
