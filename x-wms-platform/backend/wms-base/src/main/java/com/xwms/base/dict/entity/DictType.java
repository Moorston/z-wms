package com.xwms.base.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典类型实体 对字典项进行分类管理，如：入库单类型、出库单类型、库存状态等 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class DictType extends BaseEntity {

    /** 字典类型编码（唯一） */
    private String dictCode;

    /** 字典类型名称 */
    private String dictName;

    /** 字典类型描述 */
    private String description;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 排序 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
