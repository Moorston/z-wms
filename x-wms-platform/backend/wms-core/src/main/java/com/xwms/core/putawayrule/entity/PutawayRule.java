package com.xwms.core.putawayrule.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架规则定义 定义商品入库后上架到库位的策略和规则 */
@Data
@TableName("wms_putaway_rule")
public class PutawayRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型: SKU按商品/CATEGORY按品类/OWNER按货主/WAREHOUSE按仓库/GLOBAL全局 */
    private String ruleType;

    /** 适用SKU(空表示所有) */
    private String skuCode;

    /** 适用品类 */
    private String categoryCode;

    /** 适用货主 */
    private String ownerCode;

    /** 适用仓库（单仓库时使用，多仓库时用warehouseCodes） */
    private String warehouseCode;

    /** 适用仓库列表（JSON数组，支持多仓库） */
    private String warehouseCodes;

    /** 上架策略: NEAREST最近/FIFO先进先出/FEFO先到期先出/ZONE按区域/HEIGHT按高度/WEIGHT按重量 */
    private String strategy;

    /** 目标库区编码 */
    private String targetAreaCode;

    /** 目标库位组编码 */
    private String targetLocationGroup;

    /** 优先库区(ZONE策略用) */
    private String preferredArea;

    /** 商品重量(kg)(HEIGHT/WEIGHT策略用) */
    private BigDecimal productWeight;

    /** 是否允许混放: Y是/N否 */
    private String allowMix;

    /** 是否允许批次混放: Y是/N否 */
    private String allowBatchMix;

    /** 最小库位容量利用率(%) */
    private Integer minCapacityUtilization;

    /** 最大库位容量利用率(%) */
    private Integer maxCapacityUtilization;

    /** 优先级(数字越小优先级越高) */
    private Integer priority;

    /** 状态: DRAFT草稿/PENDING_REVIEW待审核/ACTIVE启用/DISABLED停用/ARCHIVED归档 */
    private String status;

    /** 版本号 */
    private Integer version;

    /** 生效日期 */
    private java.time.LocalDate effectiveDate;

    /** 失效日期 */
    private java.time.LocalDate expireDate;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;

    /** 规则行列表（非数据库字段，联合查询使用） */
    @TableField(exist = false)
    private List<PutawayRuleLine> ruleLines;
}
