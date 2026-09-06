package com.xwms.core.print.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 打印队列 */
@Data
@TableName("wms_print_queue")
public class PrintQueue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String queueNo;
    private String printerCode;
    private Long taskId;
    private String taskNo;

    /** 优先级1-10, 1最高 */
    private Integer priority;

    /** 状态: WAITING/PRINTING/DONE/FAILED */
    private String status;

    private Integer retryCount;
    private Integer maxRetry;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
}
