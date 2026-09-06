package com.xwms.common.feign.dto;

import java.io.Serializable;

import lombok.Data;

/** 货主DTO（Feign传输对象） */
@Data
public class OwnerDTO implements Serializable {
    private Long id;
    private String ownerCode;
    private String ownerName;
    private String ownerType;
    private String industry;
    private String billingType;
    private String status;
}
