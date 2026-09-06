package com.xwms.core.putaway.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架推荐日志表 记录每次库位推荐的详细信息，用于分析推荐命中率和优化规则 */
@Data
@TableName("wms_putaway_recommend_log")
public class PutawayRecommendLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日志号 */
    private String logNo;

    /** 上架任务号 */
    private String taskNo;

    /** 任务明细号 */
    private String detailNo;

    /** 仓库编码 */
    private String warehouseCode;

    /** 货主编码 */
    private String ownerCode;

    /** 商品编码 */
    private String skuCode;

    /** 批次号 */
    private String batchNo;

    /** 上架数量 */
    private BigDecimal putawayQty;

    /** 命中的上架规则ID */
    private Long ruleId;

    /** 命中的规则代码 */
    private String ruleCode;

    /** 命中的规则行号 */
    private Integer hitLineNo;

    /** 候选库位数量 */
    private Integer candidateCount;

    /** 推荐的目标库位 */
    private String recommendedLocation;

    /** 推荐库区 */
    private String recommendedArea;

    /** 推荐得分（0-100） */
    private BigDecimal recommendScore;

    /** 距离得分 */
    private BigDecimal distanceScore;

    /** 容量得分 */
    private BigDecimal capacityScore;

    /** 周转得分 */
    private BigDecimal turnoverScore;

    /** 混放得分 */
    private BigDecimal mixScore;

    /** 是否人工覆盖：Y/N */
    private String isOverride;

    /** 人工覆盖原因代码 */
    private String overrideReasonCode;

    /** 实际使用库位 */
    private String actualLocation;

    /** 响应时间（毫秒） */
    private Long responseTimeMs;

    /** 推荐时间 */
    private LocalDateTime recommendTime;

    /** 备注 */
    private String remark;
}
