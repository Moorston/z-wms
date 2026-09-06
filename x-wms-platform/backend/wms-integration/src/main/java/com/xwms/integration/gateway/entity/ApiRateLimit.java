package com.xwms.integration.gateway.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** API限流配置 */
@Data
@TableName("wms_api_rate_limit")
public class ApiRateLimit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String limitCode;
    private String limitName;

    /** 限流目标类型: API/APP/IP/GLOBAL */
    private String targetType;

    /** 限流目标值: api_code/app_key/ip/global */
    private String targetValue;

    /** 限流QPS */
    private Integer limitQps;

    /** 日调用次数限制 */
    private Integer limitDay;

    /** 突发容量 */
    private Integer burstSize;

    /** 窗口类型: FIXED固定/SLIDING滑动/TOKEN令牌桶 */
    private String windowType;

    private Integer enabled;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
