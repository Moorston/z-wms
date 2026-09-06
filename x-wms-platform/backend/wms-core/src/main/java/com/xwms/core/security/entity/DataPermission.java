package com.xwms.core.security.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 数据权限 */
@Data
@TableName("wms_data_permission")
public class DataPermission {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String permissionCode;
    private String permissionName;

    /** 权限类型: WAREHOUSE/OWNER/AREA/LOCATION/PRODUCT/DEPARTMENT */
    private String permissionType;

    /** 资源类型: INVENTORY/INBOUND/OUTBOUND/TRANSFER/STOCKTAKE/REPORT */
    private String resourceType;

    /** 范围类型: ALL/ASSIGNED/SELF/DEPARTMENT */
    private String scopeType;

    /** 范围值(JSON数组) */
    private String scopeValues;

    private String roleCode;
    private String userCode;
    private String warehouseCode;
    private String ownerCode;

    /** 可查看: Y/N */
    private String canView;

    /** 可创建: Y/N */
    private String canCreate;

    /** 可修改: Y/N */
    private String canUpdate;

    /** 可删除: Y/N */
    private String canDelete;

    /** 可导出: Y/N */
    private String canExport;

    /** 可审批: Y/N */
    private String canApprove;

    private String status;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
