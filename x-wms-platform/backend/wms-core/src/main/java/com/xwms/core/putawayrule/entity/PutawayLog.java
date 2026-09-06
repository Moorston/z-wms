package com.xwms.core.putawayrule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架执行日志 记录每次上架规则执行的结果 */
@Data
@TableName("wms_putaway_log")
public class PutawayLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日志编号 */
    private String logNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联ASN单号 */
    private String asnNo;

    /** 商品编码 */
    private String skuCode;

    /** 批次号 */
    private String batchNo;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 上架数量 */
    private java.math.BigDecimal quantity;

    /** 源库位(收货区) */
    private String sourceLocation;

    /** 目标库位 */
    private String targetLocation;

    /** 使用的规则编码 */
    private String ruleCode;

    /** 使用的策略编码 */
    private String strategyCode;

    /** 执行状态: SUCCESS成功/FAILED失败/PARTIAL部分成功 */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 尝试库位数 */
    private Integer tryCount;

    /** 执行耗时(毫秒) */
    private Long durationMs;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
