package com.xwms.core.notification.entity;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通知模板 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_notify_template")
public class NotifyTemplate extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String templateCode;
    private String templateName;

    /** 通知类型: SYSTEM系统/ALERT告警/BUSINESS业务/REMIND提醒 */
    private String notifyType;

    /** 渠道: IN_APP/SMS/EMAIL/DINGTALK/WECHAT/FEISHU */
    private String channel;

    private String titleTemplate;

    /** 内容模板, 支持${变量}占位符 */
    private String contentTemplate;

    /** 变量定义(JSON) */
    private String variables;

    private Integer enabled;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
