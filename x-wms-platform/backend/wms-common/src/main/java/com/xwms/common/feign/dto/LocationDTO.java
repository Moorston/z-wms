package com.xwms.common.feign.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

/** 库位DTO（Feign传输对象） */
@Data
public class LocationDTO implements Serializable {
    private Long id;
    private String locationCode;
    private String warehouseCode;
    private String areaCode;
    private String locationGroup;
    private String rowNo;
    private String columnNo;
    private String levelNo;
    private String locationType;
    private String temperatureZone;
    private String status;
    private BigDecimal capacity;
    private BigDecimal usedCapacity;
    private Integer sortNo;
    private Integer coordX;
    private Integer coordY;
    private Integer coordZ;
}
