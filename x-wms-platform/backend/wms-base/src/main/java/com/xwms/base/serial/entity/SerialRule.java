package com.xwms.base.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 序列号规则 */
@Data
@TableName("wms_serial_rule")
public class SerialRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 适用SKU(空表示所有) */
    private String skuCode;

    /** 适用品类 */
    private String categoryCode;

    private String prefix;
    private String suffix;

    /** 序列长度 */
    private Integer seqLength;

    /** 起始序号 */
    private Long startSeq;

    /** 当前序号 */
    private Long currentSeq;

    /** 是否校验位 */
    private Integer checkDigit;

    /** 日期格式 */
    private String dateFormat;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
