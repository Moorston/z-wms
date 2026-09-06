package com.xwms.core.abc.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 分类历史 */
@Data
@TableName("wms_abc_history")
public class AbcHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String historyId;
    private String warehouseCode;
    private String skuCode;

    /** 原ABC分类 */
    private String oldAbcClass;

    /** 新ABC分类 */
    private String newAbcClass;

    /** 原XYZ分类 */
    private String oldXyzClass;

    /** 新XYZ分类 */
    private String newXyzClass;

    /** 变化类型: UPGRADE/DOWNGRADE/UNCHANGED */
    private String changeType;

    private String changeReason;
    private String operator;
    private LocalDateTime changeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
