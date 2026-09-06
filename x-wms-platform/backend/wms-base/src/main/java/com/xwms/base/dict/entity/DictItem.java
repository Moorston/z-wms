package com.xwms.base.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典项实体 具体的字典值，如：入库单类型下的"采购入库"、"退货入库"等 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class DictItem extends BaseEntity {

    /** 所属字典类型编码 */
    private String dictCode;

    /** 字典项值（存储在业务表中的值） */
    private String itemValue;

    /** 字典项标签（展示给用户的名称） */
    private String itemLabel;

    /** 字典项描述 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 父级字典项值（用于级联字典，如省市区） */
    private String parentValue;

    /** 扩展属性（JSON格式，如颜色、图标等） */
    private String extAttrs;

    /** 备注 */
    private String remark;
}
