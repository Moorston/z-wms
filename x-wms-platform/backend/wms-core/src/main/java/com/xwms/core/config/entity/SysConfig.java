package com.xwms.core.config.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 系统参数配置表 支持按模块/分类管理系统参数，支持仓库级/货主级参数覆盖 */
@Data
@TableName("wms_sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参数编码（唯一） */
    private String configCode;

    /** 参数名称 */
    private String configName;

    /** 参数值 */
    private String configValue;

    /** 参数默认值 */
    private String defaultValue;

    /** 参数类型：STRING字符串/NUMBER数字/BOOLEAN布尔/JSON对象/DATE日期 */
    private String configType;

    /** 参数分类：INBOUND入库/OUTBOUND出库/INVENTORY库存/QC质检/SYSTEM系统/INTERFACE接口 */
    private String category;

    /** 模块编码 */
    private String moduleCode;

    /** 仓库编码（为空表示全局参数） */
    private String warehouseCode;

    /** 货主编码（为空表示全局参数） */
    private String ownerCode;

    /** 是否启用：Y/N */
    private String enabled;

    /** 是否系统内置：Y/N（内置参数不允许删除） */
    private String isSystem;

    /** 是否允许修改：Y/N */
    private String allowModify;

    /** 校验规则（正则表达式或枚举值） */
    private String validateRule;

    /** 参数描述 */
    private String description;

    /** 备注 */
    private String remark;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
