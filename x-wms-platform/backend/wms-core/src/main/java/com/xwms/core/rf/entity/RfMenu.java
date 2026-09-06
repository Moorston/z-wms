package com.xwms.core.rf.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** RF菜单配置 */
@Data
@TableName("wms_rf_menu")
public class RfMenu {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String menuCode;
    private String menuName;
    private String parentCode;

    /** 菜单类型: MENU菜单/FUNCTION功能 */
    private String menuType;

    /** 关联任务类型 */
    private String taskType;

    private String icon;
    private Integer sortOrder;
    private Integer enabled;

    /** 权限编码 */
    private String permissionCode;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
