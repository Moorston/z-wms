package com.xwms.base.location.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 库位实体 最小存储单元，编码规则：仓库-库区-排-列-层 如 WH01-A-01-02-03 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
public class Location extends BaseEntity {
    /** 库位编码 */
    private String locationCode;

    /** 库位名称（虚拟库位展示用，非持久化） */
    @TableField(exist = false)
    private String locationName;

    /** 所属仓库 */
    private String warehouseCode;

    /** 所属库区 */
    private String areaCode;

    /** 库位组（用于批量管理） */
    private String locationGroup;

    /** 排 */
    private String rowNo;

    /** 列 */
    private String columnNo;

    /** 层 */
    private String levelNo;

    /** 库位类型：STORAGE/PICK/RECEIVE/SHIP/QC */
    private String locationType;

    /** 温区 */
    private String temperatureZone;

    /** 状态：EMPTY/NORMAL/FULL/FROZEN/DISABLED */
    private String status;

    /** 容量（体积） */
    private BigDecimal capacity;

    /** 已用容量 */
    private BigDecimal usedCapacity;

    /** 承重（kg） */
    private BigDecimal maxWeight;

    /** 排序号（路径规划用） */
    private Integer sortNo;

    /** X坐标（路径规划用） */
    private Integer coordX;

    /** Y坐标（路径规划用） */
    private Integer coordY;

    /** Z坐标（路径规划用） */
    private Integer coordZ;

    /** 库位属性（JSON，如是否允许混批/混SKU） */
    private String locationAttrs;

    // ========== 虚拟库位扩展字段 ==========
    /** 是否虚拟库位：Y/N */
    private String isVirtual;

    /** 虚拟库位类型：SORTING/GRID/DIFFERENCE/STAGING/IN_TRANSIT/SYSTEM */
    private String virtualType;

    /** 所属播种墙ID（格口虚拟库位用） */
    private String sortingWallCode;

    /** 格口号（格口虚拟库位用） */
    private String gridNo;

    /** 绑定的订单号（格口虚拟库位用） */
    private String boundOrderNo;

    /** 绑定的波次号（分拣中虚拟库位用） */
    private String boundWaveNo;

    /** 逻辑容量限制（虚拟库位可设，0表示不限制） */
    private java.math.BigDecimal virtualCapacity;

    /** 是否自动释放（分拣完成后自动变空闲）：Y/N */
    private String autoRelease;

    /** 关联业务单号（调拨单号/移库单号等） */
    private String refBizNo;
}
