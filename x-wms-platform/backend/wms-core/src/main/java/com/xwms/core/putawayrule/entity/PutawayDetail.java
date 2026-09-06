package com.xwms.core.putawayrule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架执行详情 记录每次上架的详细库位分配信息 */
@Data
@TableName("wms_putaway_detail")
public class PutawayDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 详情编号 */
    private String detailNo;

    /** 关联日志编号 */
    private String logNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 商品编码 */
    private String skuCode;

    /** 批次号 */
    private String batchNo;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 源库位 */
    private String sourceLocation;

    /** 目标库位 */
    private String targetLocation;

    /** 目标库区 */
    private String targetArea;

    /** 上架数量 */
    private java.math.BigDecimal quantity;

    /** 库位原数量 */
    private java.math.BigDecimal originalQty;

    /** 库位现数量 */
    private java.math.BigDecimal currentQty;

    /** 库位容量利用率(%) */
    private Integer capacityUtilization;

    /** 是否为推荐库位: Y是/N否 */
    private String isRecommend;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态: PENDING待执行/EXECUTING执行中/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 操作人 */
    private String operator;

    /** 推荐策略信息（非持久化，用于展示） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String remark;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
