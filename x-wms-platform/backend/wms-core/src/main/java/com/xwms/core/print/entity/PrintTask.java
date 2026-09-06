package com.xwms.core.print.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 打印任务 */
@Data
@TableName("wms_print_task")
public class PrintTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String templateCode;
    private String templateName;

    /** 业务类型: PICKING/OUTBOUND/INBOUND等 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    private String printerCode;
    private String printerName;

    /** 打印份数 */
    private Integer copies;

    /** 打印数据(JSON) */
    private String printData;

    /** 渲染后的打印内容 */
    private String printContent;

    /** 状态: PENDING/PRINTING/SUCCESS/FAILED/CANCELLED */
    private String status;

    private String failReason;
    private LocalDateTime printTime;
    private String createdBy;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
