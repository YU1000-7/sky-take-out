package com.sky.service.impl;

import com.sky.common.context.BaseContext;
import com.sky.common.exception.BusinessException;
import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressBookServiceImplTest {

    @Mock
    private AddressBookMapper addressBookMapper;

    @InjectMocks
    private AddressBookServiceImpl addressBookService;

    @AfterEach
    void clearContext() {
        BaseContext.removeCurrentId();
    }

    @Test
    void firstAddressShouldBecomeDefault() {
        BaseContext.setCurrentId(9L);
        when(addressBookMapper.selectCount(any())).thenReturn(0L);

        AddressBookDTO dto = new AddressBookDTO();
        dto.setConsignee("张三");
        dto.setPhone("13800138000");
        dto.setDetail("测试地址");
        dto.setIsDefault(0);
        addressBookService.add(dto);

        ArgumentCaptor<AddressBook> captor = ArgumentCaptor.forClass(AddressBook.class);
        verify(addressBookMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals(1, captor.getValue().getIsDefault());
    }

    @Test
    void shouldRejectAddressOwnedByAnotherUser() {
        BaseContext.setCurrentId(9L);
        AddressBook addressBook = new AddressBook();
        addressBook.setId(1L);
        addressBook.setUserId(10L);
        when(addressBookMapper.selectById(1L)).thenReturn(addressBook);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> addressBookService.getById(1L)
        );
        assertEquals("无权操作该地址", exception.getMessage());
    }
}
