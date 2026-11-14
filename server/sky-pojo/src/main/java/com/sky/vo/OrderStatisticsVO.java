package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderStatisticsVO implements Serializable {

    private List<DateCountVO> orderCountList = new ArrayList<>();
    private List<DateCountVO> validOrderCountList = new ArrayList<>();
}
