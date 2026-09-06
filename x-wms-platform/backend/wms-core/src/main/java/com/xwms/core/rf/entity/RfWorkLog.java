package com.xwms.core.rf.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** RF作业记录 */
@Data
@TableName("wms_rf_work_log")
public class RfWorkLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private Long taskId;
    private String taskNo;
    private String taskType;
    private String userId;
    private String userName;
    private String pdaDeviceId;

    /** 动作: LOGIN/LOGOUT/ACCEPT/START/SCAN/CONFIRM/PAUSE/RESUME/COMPLETE/CANCEL/EXCEPTION */
    private String action;

    private String locationFrom;
    private String locationTo;
    private String sku;
    private String batchNo;
    private String barcode;
    private BigDecimal quantity;

    private String beforeStatus;
    private String afterStatus;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
