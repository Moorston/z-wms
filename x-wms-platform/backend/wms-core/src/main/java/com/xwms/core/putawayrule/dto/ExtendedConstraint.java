package com.xwms.core.putawayrule.dto;

import lombok.Data;

/**
 * 扩展约束DTO
 * 5种约束类型：LOCATION_TYPE库位类型/LOCATION_ATTR库位属性/CYCLE_ZONE周转区/LOCATION_GROUP库位组/SKU_LOCATION_LIMIT指定库区产品库位个数限制
 */
@Data
public class ExtendedConstraint {

    /** 约束类型 */
    private String type;

    /** 约束值（LOCATION_TYPE/CYCLE_ZONE/LOCATION_GROUP类型时为对应编码） */
    private String value;

    /** 最大库位数（SKU_LOCATION_LIMIT类型时使用） */
    private Integer maxLocations;
}
