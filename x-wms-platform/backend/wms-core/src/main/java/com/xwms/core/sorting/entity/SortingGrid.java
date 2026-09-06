package com.xwms.core.sorting.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 播种墙格口实体 每个格口绑定一个订单和一个虚拟库位 二次分拣时，商品按订单分播到对应格口 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_sorting_grid")
public class SortingGrid extends BaseEntity {

    /** 所属播种墙编码 */
    private String wallCode;

    /** 格口号（01-99） */
    private String gridNo;

    /** 关联的虚拟库位编码（SORT-GRID-{wallCode}-{gridNo}） */
    private String locationCode;

    /** 绑定的订单号 */
    private String boundOrderNo;

    /** 绑定的波次号 */
    private String boundWaveNo;

    /** 状态：EMPTY(空闲)/BOUND(已绑定)/SORTING(分拣中)/COMPLETED(已完成) */
    private String status;

    /** 期望数量（订单总需求数量） */
    private BigDecimal expectedQty;

    /** 实际已分数量 */
    private BigDecimal actualQty;

    /** 格口行号（展示用） */
    private Integer rowIndex;

    /** 格口列号（展示用） */
    private Integer colIndex;

    /** 电子标签地址（PTL） */
    private String ptlAddress;
}
