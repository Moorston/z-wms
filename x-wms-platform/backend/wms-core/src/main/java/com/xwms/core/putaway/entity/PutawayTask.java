package com.xwms.core.putaway.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 上架任务表 从收货完成的入库单生成上架任务，支持多种上架方式
 * 状态流转：PENDING待上架→PUTAWAYING上架中→PARTIAL部分上架→COMPLETED上架完成→CANCELLED已取消
 */
@Data
@TableName("wms_putaway_task")
public class PutawayTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上架任务号 */
    private String taskNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联收货任务号 */
    private String receiptTaskNo;

    /** 上架方式：STANDARD标准/QUICK快捷/MERGE合并/BATCH批量/LPN按箱码/DIRECT直接收货到库位/RESERVATION码盘预约 */
    private String putawayType;

    /** 上架策略：NEAREST最近距离/FIFO先进先出/FEFO先到期先出/ZONE区域优先/HEIGHT重货低层/WEIGHT重货就近 */
    private String putawayStrategy;

    /** 供应商编码 */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 收货库区（源库区） */
    private String sourceArea;

    /** 收货库位（源库位） */
    private String sourceLocation;

    /** 目标库区（推荐） */
    private String targetArea;

    /** 目标库位（推荐） */
    private String targetLocation;

    /** 预期数量 */
    private BigDecimal expectedQty;

    /** 已上架数量 */
    private BigDecimal putawayQty;

    /** 差异数量 */
    private BigDecimal differenceQty;

    /** 状态：PENDING/PUTAWAYING/PARTIAL/COMPLETED/CANCELLED */
    private String status;

    /** 是否需要质检：Y/N */
    private String qcRequired;

    /** 质检状态：NOT_REQUIRED/PENDING/PASSED/FAILED */
    private String qcStatus;

    /** 是否系统推荐库位：Y/N */
    private String systemRecommend;

    /** 是否允许修改库位：Y/N */
    private String allowLocationChange;

    /** 是否允许修改数量：Y/N */
    private String allowQtyChange;

    /** 托盘号/LPN号（码盘上架时） */
    private String lpnNo;

    /** 工作区编码（任务派发维度） */
    private String workZone;

    /** 巷道号 */
    private String aisleNo;

    /** 优先级：1-紧急 2-高 3-普通 4-低 */
    private Integer priority;

    /** 指派人（派发人） */
    private String assigner;

    /** 指派时间 */
    private LocalDateTime assignTime;

    /** 领取人（执行人） */
    private String assignee;

    /** 领取时间 */
    private LocalDateTime claimTime;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 上架人 */
    private String operator;

    /** 异常原因代码 */
    private String exceptionReasonCode;

    /** 异常描述 */
    private String exceptionRemark;

    /** 备注 */
    private String remark;

    /** 来源：RECEIPT收货/DIRECT直接/MANUAL手工 */
    private String source;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
