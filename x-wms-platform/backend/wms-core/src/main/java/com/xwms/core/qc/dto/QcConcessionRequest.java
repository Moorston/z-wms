package com.xwms.core.qc.dto;

import java.math.BigDecimal;

import lombok.Data;

/** 让步接收申请请求 */
@Data
public class QcConcessionRequest {
    private Long qcId;
    private BigDecimal qty;
    private String reason;
    private String defectDesc;
    private String impactLevel; // LOW/MEDIUM/HIGH
    private String applicant;
}
