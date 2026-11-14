package com.sky.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.common.context.BaseContext;
import com.sky.common.exception.BusinessException;
import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import com.sky.vo.AddressBookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressBookServiceImpl implements AddressBookService {

    private final AddressBookMapper addressBookMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AddressBookDTO dto) {
        Long userId = requireCurrentUserId();
        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(dto, addressBook);
        addressBook.setId(null);
        addressBook.setUserId(userId);

        boolean hasAddress = addressBookMapper.selectCount(Wrappers.<AddressBook>lambdaQuery()
                .eq(AddressBook::getUserId, userId)) > 0;
        boolean makeDefault = !hasAddress || Integer.valueOf(1).equals(dto.getIsDefault());
        if (makeDefault) {
            clearDefault(userId);
            addressBook.setIsDefault(1);
        } else {
            addressBook.setIsDefault(0);
        }
        addressBookMapper.insert(addressBook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AddressBookDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("地址 ID 不能为空");
        }
        Long userId = requireCurrentUserId();
        AddressBook existing = requireOwnedAddress(dto.getId(), userId);

        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(dto, addressBook);
        addressBook.setId(existing.getId());
        addressBook.setUserId(userId);
        addressBook.setCreateTime(existing.getCreateTime());

        if (Integer.valueOf(1).equals(dto.getIsDefault())) {
            clearDefault(userId);
            addressBook.setIsDefault(1);
        } else {
            addressBook.setIsDefault(Integer.valueOf(1).equals(existing.getIsDefault()) ? 1 : 0);
        }
        addressBookMapper.updateById(addressBook);
    }

    @Override
    public void delete(Long id) {
        Long userId = requireCurrentUserId();
        AddressBook addressBook = requireOwnedAddress(id, userId);
        addressBookMapper.deleteById(addressBook.getId());
    }

    @Override
    public List<AddressBookVO> list() {
        Long userId = requireCurrentUserId();
        List<AddressBook> records = addressBookMapper.selectList(Wrappers.<AddressBook>lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .orderByDesc(AddressBook::getIsDefault)
                .orderByDesc(AddressBook::getUpdateTime));
        return records.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AddressBookVO getById(Long id) {
        return toVO(requireOwnedAddress(id, requireCurrentUserId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        Long userId = requireCurrentUserId();
        AddressBook addressBook = requireOwnedAddress(id, userId);
        clearDefault(userId);
        addressBook.setIsDefault(1);
        addressBookMapper.updateById(addressBook);
    }

    private AddressBook requireOwnedAddress(Long id, Long userId) {
        AddressBook addressBook = addressBookMapper.selectById(id);
        if (addressBook == null) {
            throw new BusinessException("地址不存在");
        }
        if (!userId.equals(addressBook.getUserId())) {
            throw new BusinessException("无权操作该地址");
        }
        return addressBook;
    }

    private Long requireCurrentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return userId;
    }

    private void clearDefault(Long userId) {
        AddressBook update = new AddressBook();
        update.setIsDefault(0);
        addressBookMapper.update(update, Wrappers.<AddressBook>lambdaUpdate()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getIsDefault, 1));
    }

    private AddressBookVO toVO(AddressBook addressBook) {
        AddressBookVO addressBookVO = new AddressBookVO();
        BeanUtils.copyProperties(addressBook, addressBookVO);
        return addressBookVO;
    }
}
