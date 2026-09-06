package com.xwms.core.putawayrule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架策略定义 定义具体的上架执行策略和参数 */
@Data
@TableName("wms_putaway_strategy")
public class PutawayStrategy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 策略编码 */
    private String strategyCode;

    /** 策略名称 */
    private String strategyName;

    /** 策略类型: NEAREST最近距离/FIFO先进先出/FEFO先到期先出/ZONE区域优先/HEIGHT高度优先/WEIGHT重量优先/EMPTY空库位优先/FULL满库位优先 */
    private String strategyType;

    /** 关联规则编码 */
    private String ruleCode;

    /** 排序字段: DISTANCE距离/EXPIRY_DATE效期/LOCATION_CODE库位编码/CAPACITY容量 */
    private String sortField;

    /** 排序方向: ASC升序/DESC降序 */
    private String sortDirection;

    /** 过滤条件JSON */
    private String filterCondition;

    /** 最大尝试库位数 */
    private Integer maxTryLocations;

    /** 是否启用动态库位: Y是/N否 */
    private String enableDynamicLocation;

    /** 是否启用推荐库位: Y是/N否 */
    private String enableRecommendLocation;

    /** 状态: ACTIVE启用/INACTIVE禁用 */
    private String status;

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
}
