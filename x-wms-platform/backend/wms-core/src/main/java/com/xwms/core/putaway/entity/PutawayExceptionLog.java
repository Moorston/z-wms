package com.xwms.core.putaway.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架例外日志表 记录上架过程中的异常和人工覆盖操作，用于后续分析和规则优化 */
@Data
@TableName("wms_putaway_exception_log")
public class PutawayExceptionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日志号 */
    private String logNo;

    /** 上架任务号 */
    private String taskNo;

    /** 任务明细号 */
    private String detailNo;

    /** 仓库编码 */
    private String warehouseCode;

    /** 商品编码 */
    private String skuCode;

    /**
     * 异常类型：NO_LOCATION无可用库位/LOCATION_FULL库位已满/LOCATION_LOCKED库位锁定/
     * OVERRIDE人工覆盖/QTY_DIFFERENCE数量差异/BATCH_MISMATCH批次不匹配/OTHER其他
     */
    private String exceptionType;

    /** 原因代码 */
    private String reasonCode;

    /** 原因描述 */
    private String reasonDesc;

    /** 原始推荐库位 */
    private String originalLocation;

    /** 实际使用库位 */
    private String actualLocation;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 处理状态：PENDING待处理/PROCESSING处理中/RESOLVED已解决/IGNORED已忽略 */
    private String handleStatus;

    /** 处理人 */
    private String handler;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理结果 */
    private String handleResult;

    /** 备注 */
    private String remark;
}
