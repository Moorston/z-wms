package com.xwms.core.sorting.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 播种墙实体 二次分拣（播种式）的物理设备，包含多个格口 每个格口对应一个订单，格口绑定虚拟库位 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_sorting_wall")
public class SortingWall extends BaseEntity {

    /** 播种墙编码 */
    private String wallCode;

    /** 播种墙名称 */
    private String wallName;

    /** 所属仓库 */
    private String warehouseCode;

    /** 所属库区（分拣区） */
    private String areaCode;

    /** 格口总数 */
    private Integer gridCount;

    /** 行数 */
    private Integer gridRows;

    /** 列数 */
    private Integer gridCols;

    /** 状态：IDLE(空闲)/SORTING(分拣中)/MAINTENANCE(维护中) */
    private String status;

    /** 当前处理波次号 */
    private String currentWaveNo;

    /** 电子标签设备ID（可选，对接PTL电子标签系统） */
    private String ptlDeviceId;

    /** 备注 */
    private String remark;
}
