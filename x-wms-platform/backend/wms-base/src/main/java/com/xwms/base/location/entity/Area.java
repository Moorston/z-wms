package com.xwms.base.location.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 库区实体 仓库下的分区，如收货区/存储区/拣货区/发货区/退货区 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_area")
public class Area extends BaseEntity {
    /** 库区编码 */
    private String areaCode;

    /** 库区名称 */
    private String areaName;

    /** 所属仓库 */
    private String warehouseCode;

    /** 库区类型：RECEIVE/STORAGE/PICK/SHIP/RETURN/QC */
    private String areaType;

    /** 温区：FROZEN/CHILLED/CONSTANT/NORMAL */
    private String temperatureZone;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 排序号 */
    private Integer sortNo;
}
