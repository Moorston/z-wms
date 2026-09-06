package com.xwms.core.qc.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 抽样方案 (GB/T 2828.1) */
@Data
@TableName("wms_qc_sampling_plan")
public class QcSamplingPlan {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 方案编码 */
    private String planCode;

    /** 检验水平 */
    private String inspectionLevel;

    /** 批量下限 */
    private Integer lotSizeFrom;

    /** 批量上限 */
    private Integer lotSizeTo;

    /** 样本量字码 */
    private String sampleSizeCode;

    /** 样本量 */
    private Integer sampleSize;

    /** AQL 0.065 接收数 */
    private Integer aql0065Accept;

    private Integer aql0065Reject;

    /** AQL 0.10 */
    private Integer aql010Accept;

    private Integer aql010Reject;

    /** AQL 0.15 */
    private Integer aql015Accept;

    private Integer aql015Reject;

    /** AQL 0.25 */
    private Integer aql025Accept;

    private Integer aql025Reject;

    /** AQL 0.40 */
    private Integer aql040Accept;

    private Integer aql040Reject;

    /** AQL 0.65 */
    private Integer aql065Accept;

    private Integer aql065Reject;

    /** AQL 1.0 */
    private Integer aql10Accept;

    private Integer aql10Reject;

    /** AQL 1.5 */
    private Integer aql15Accept;

    private Integer aql15Reject;

    /** AQL 2.5 */
    private Integer aql25Accept;

    private Integer aql25Reject;

    /** AQL 4.0 */
    private Integer aql40Accept;

    private Integer aql40Reject;

    /** AQL 6.5 */
    private Integer aql65Accept;

    private Integer aql65Reject;

    /** 严格度: NORMAL/REDUCED/TIGHTENED */
    private String strictness;
}
