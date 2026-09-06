package com.xwms.core.rf.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** RF用户会话 */
@Data
@TableName("wms_rf_session")
public class RfSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String sessionId;
    private String userId;
    private String userName;
    private String pdaDeviceId;
    private String warehouseCode;

    private LocalDateTime loginTime;
    private LocalDateTime lastActiveTime;
    private LocalDateTime logoutTime;

    /** 状态: ACTIVE活跃/IDLE空闲/EXPIRED过期/LOGOUT登出 */
    private String status;

    private Long currentTaskId;
    private String currentMenu;
    private String ipAddress;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
