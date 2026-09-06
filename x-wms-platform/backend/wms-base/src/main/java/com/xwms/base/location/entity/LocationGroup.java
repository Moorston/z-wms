package com.xwms.base.location.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 库位组实体 用于批量管理一组库位，如按货架/巷道/区域分组 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location_group")
public class LocationGroup extends BaseEntity {
    /** 库位组编码 */
    private String groupCode;

    /** 库位组名称 */
    private String groupName;

    /** 所属仓库 */
    private String warehouseCode;

    /** 所属库区 */
    private String areaCode;

    /** 库位组类型：SHELF货架/AISLE巷道/ZONE区域 */
    private String groupType;

    /** 库位数量 */
    private Integer locationCount;

    /** 排序号 */
    private Integer sortNo;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 备注 */
    private String remark;
}
