package com.xwms.common.feign.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Data;

/** 商品DTO（Feign传输对象） */
@Data
public class ProductDTO implements Serializable {
    private Long id;
    private String sku;
    private String productName;
    private String ownerCode;
    private String barcode;
    private String category;
    private String brand;
    private String spec;
    private String unit;
    private BigDecimal weight;
    private BigDecimal volume;
    private String temperatureZone;
    private Integer shelfLifeDays;
    private Boolean batchManaged;
    private Boolean serialManaged;
    private String putawayRule;
    private String allocationRule;
    private String rotationRule;
    private String status;
}
