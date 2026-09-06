package com.xwms.core.sorting.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 分拣差异实体 二次分拣过程中发现的多货/少货/错货等异常 差异商品存放在差异虚拟库位中 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_sorting_difference")
public class SortingDifference extends BaseEntity {

    /** 差异单号 */
    private String diffNo;

    /** 波次号 */
    private String waveNo;

    /** 订单号（少货/错货时关联） */
    private String orderNo;

    /** 商品SKU */
    private String sku;

    /** 批次号 */
    private String batchNo;

    /** 差异类型：MORE(多货)/LESS(少货)/WRONG(错货)/DAMAGE(破损) */
    private String differenceType;

    /** 差异数量 */
    private BigDecimal differenceQty;

    /** 状态：PENDING(待处理)/RESOLVED(已处理) */
    private String status;

    /** 处理方式：RETURN(退回存储)/SCRAP(报废)/REPLENISH(补拣) */
    private String handleMethod;

    /** 差异原因 */
    private String reason;

    /** 处理人 */
    private String handler;

    /** 处理时间 */
    private java.time.LocalDateTime handledAt;

    /** 差异虚拟库位编码 */
    private String diffLocationCode;
}
