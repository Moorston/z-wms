package com.xwms.core.putawayrule.dto;

import lombok.Data;

/**
 * 库位限制DTO
 * 7种限制类型：EMPTY_BIN/NO_MIX_SKU/NO_MIX_LOT/SAME_SKU/SAME_LOT/SAME_PRODUCT_GROUP/DEEP_LANE_FIFO
 */
@Data
public class LocationLimit {

    /** 限制类型 */
    private String type;

    /** 限制值（SAME_PRODUCT_GROUP类型时为产品组编码） */
    private String value;
}
