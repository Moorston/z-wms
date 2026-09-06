package com.xwms.core.print.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 打印机 */
@Data
@TableName("wms_printer")
public class Printer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String printerCode;
    private String printerName;

    /** 打印机类型: LASER/INKJET/THERMAL/LABEL */
    private String printerType;

    private String printerModel;
    private String ipAddress;
    private Integer port;

    /** 连接类型: NETWORK/USB/SERIAL */
    private String connectionType;

    /** 所属仓库 */
    private String warehouseCode;

    /** 位置描述 */
    private String location;

    /** 支持纸张大小(逗号分隔) */
    private String paperSizes;

    /** 状态: ONLINE/OFFLINE/ERROR/MAINTENANCE */
    private String status;

    private LocalDateTime lastHeartbeat;
    private Integer enabled;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
