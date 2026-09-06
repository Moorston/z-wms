package com.xwms.core.print.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 打印模板 */
@Data
@TableName("wms_print_template")
public class PrintTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String templateCode;
    private String templateName;

    /** 模板类型: PICKING/OUTBOUND/INBOUND/LABEL等 */
    private String templateType;

    /** 纸张大小: A4/A5/100x100/80x60等 */
    private String paperSize;

    /** 方向: PORTRAIT纵向/LANDSCAPE横向 */
    private String orientation;

    /** 模板内容(HTML/XML/Jasper) */
    private String templateContent;

    /** 模板引擎: HTML/JASPER/FREEMARKER */
    private String templateEngine;

    /** 可用变量(JSON数组) */
    private String variables;

    /** 默认打印机 */
    private String defaultPrinter;

    /** 默认份数 */
    private Integer copies;

    private Integer enabled;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
