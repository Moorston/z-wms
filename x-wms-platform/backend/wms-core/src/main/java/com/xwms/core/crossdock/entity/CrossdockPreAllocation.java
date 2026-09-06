package com.xwms.core.crossdock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 越库预配表 ASN到货前，根据越库规则为等待的出库SO进行预先匹配 */
@Data
@TableName("wms_crossdock_pre_alloc")
public class CrossdockPreAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预配单号 */
    private String allocNo;

    /** 越库单号 */
    private String crossdockNo;

    /** ASN号 */
    private String asnNo;

    /** 入库单号 */
    private String inboundNo;

    /** 出库单号（SO） */
    private String outboundNo;

    /** 波次号 */
    private String waveNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 预配数量 */
    private BigDecimal allocQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已分配数量 */
    private BigDecimal allocatedQty;

    /** 播种位编码 */
    private String sowingLocation;

    /** 出库月台 */
    private String outboundDock;

    /** 匹配类型：EXACT精确匹配/FUZZY模糊匹配/MANUAL手动匹配 */
    private String matchType;

    /** 匹配规则编码 */
    private String ruleCode;

    /** 状态：PENDING待预配/ALLOCATED已预配/RECEIVED已收货/ALLOCATED_OUT已分配出库/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 优先级 */
    private Integer priority;

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
