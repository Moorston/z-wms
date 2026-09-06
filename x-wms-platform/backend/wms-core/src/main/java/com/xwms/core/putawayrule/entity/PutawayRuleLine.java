package com.xwms.core.putawayrule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架规则行表 每条上架规则包含多行规则行，按行号顺序执行，支持条件匹配、规则代码、库位限制、空间限制、扩展约束和跳转配置 */
@Data
@TableName("wms_putaway_rule_line")
public class PutawayRuleLine {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则头ID */
    private Long ruleId;

    /** 行号（1-100，规则内唯一） */
    private Integer lineNo;

    /** 行描述 */
    private String description;

    /**
     * 行条件JSON：{"orderType":"NORMAL","packageLevel":"CASE","cycleLevel":"A","batchAttr":{"key":"origin","value":"广东"}}
     */
    private String conditionJson;

    /** 规则代码（01-31） */
    private String ruleCode;

    /** 目标库区（规则代码02/03/21时必填） */
    private String targetZone;

    /** 目标库位（规则代码01/04/07/22时必填） */
    private String targetLocation;

    /** 库位限制JSON数组：[{"type":"NO_MIX_LOT"},{"type":"SAME_PRODUCT_GROUP","value":"GROUP001"}] */
    private String locationLimitsJson;

    /** 空间限制JSON数组：[{"type":"VOLUME","threshold":2.5},{"type":"WEIGHT","threshold":1000}] */
    private String spaceLimitsJson;

    /**
     * 扩展约束JSON数组：[{"type":"CYCLE_ZONE","value":"A"},{"type":"SKU_LOCATION_LIMIT","maxLocations":5}]
     */
    private String extendedConstraintsJson;

    /** 成功跳转行号（为空则规则结束） */
    private Integer successJumpLine;

    /** 失败跳转行号（为空则下一行） */
    private Integer failJumpLine;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
