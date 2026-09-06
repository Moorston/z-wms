package com.xwms.core.qc.dto;

import java.math.BigDecimal;
import java.util.List;

import com.xwms.core.qc.entity.QcItem;

import lombok.Data;

/** 提交质检结果请求 */
@Data
public class QcSubmitRequest {
    private Long qcId;
    private BigDecimal inspectedQty;
    private BigDecimal qualifiedQty;
    private Integer defectCount;
    private String unqualifiedType;
    private String defectDesc;
    private List<QcItem> items;
}
