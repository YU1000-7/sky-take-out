package com.sky.service;

import com.sky.dto.AddressBookDTO;
import com.sky.vo.AddressBookVO;

import java.util.List;

public interface AddressBookService {

    void add(AddressBookDTO dto);

    void update(AddressBookDTO dto);

    void delete(Long id);

    List<AddressBookVO> list();

    AddressBookVO getById(Long id);

    void setDefault(Long id);
}
