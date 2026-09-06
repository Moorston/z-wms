package com.xwms.core.putaway.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架原因代码表 用于人工覆盖推荐库位、异常上报等场景的原因代码管理 */
@Data
@TableName("wms_putaway_reason_code")
public class PutawayReasonCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原因代码 */
    private String reasonCode;

    /** 原因名称 */
    private String reasonName;

    /** 原因类型：OVERRIDE人工覆盖/EXCEPTION异常/DIFFERENCE差异 */
    private String reasonType;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态：ACTIVE启用/DISABLED停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
