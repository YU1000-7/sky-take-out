package com.sky.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
public class AddressBookDTO implements Serializable {

    private Long id;

    @NotBlank(message = "收货人不能为空")
    private String consignee;

    private String sex;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;

    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private String label;
    private Integer isDefault;
}
