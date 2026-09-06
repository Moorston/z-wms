package com.xwms.core.qc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 质检样本表 记录抽样检验的样本信息 */
@Data
@TableName("wms_qc_sample")
public class QcSample {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 样本号 */
    private String sampleNo;

    /** 关联质检任务号 */
    private String taskNo;

    /** 关联质检单号 */
    private String qcNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 序列号 */
    private String serialNo;

    /** 样本数量 */
    private BigDecimal sampleQty;

    /** 样本状态：DRAWN已抽取/TESTING检测中/PASSED合格/FAILED不合格/RETURNED已归还/DESTROYED已销毁 */
    private String status;

    /** 缺陷类型：CRITICAL严重缺陷/MAJOR主要缺陷/MINOR次要缺陷 */
    private String defectType;

    /** 缺陷描述 */
    private String defectDesc;

    /** 检测项目 */
    private String testItem;

    /** 检测标准 */
    private String testStandard;

    /** 检测方法 */
    private String testMethod;

    /** 检测设备 */
    private String testDevice;

    /** 检测值 */
    private String testValue;

    /** 标准值上限 */
    private String upperLimit;

    /** 标准值下限 */
    private String lowerLimit;

    /** 是否合格：Y/N */
    private String isQualified;

    /** 抽样人 */
    private String drawnBy;

    /** 抽样时间 */
    private LocalDateTime drawnTime;

    /** 检测人 */
    private String testedBy;

    /** 检测时间 */
    private LocalDateTime testedTime;

    /** 归还人 */
    private String returnedBy;

    /** 归还时间 */
    private LocalDateTime returnedTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
