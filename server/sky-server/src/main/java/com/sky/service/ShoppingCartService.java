package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.vo.ShoppingCartVO;

import java.util.List;

public interface ShoppingCartService {

    void add(ShoppingCartDTO dto);

    void sub(ShoppingCartDTO dto);

    List<ShoppingCartVO> list();

    void clean();
}
