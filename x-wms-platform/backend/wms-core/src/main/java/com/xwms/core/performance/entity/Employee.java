package com.xwms.core.performance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 人员档案 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_employee")
public class Employee extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String employeeNo;
    private String employeeName;
    private String gender;
    private String phone;
    private String idCard;
    private String warehouseCode;

    /** 部门: 收货组/上架组/拣货组/复核组/打包组/盘点组 */
    private String department;

    /** 岗位: 收货员/上架员/拣货员/复核员/打包员/盘点员/班长/主管 */
    private String position;

    /** 员工类型: 正式工/临时工/外包工 */
    private String employeeType;

    /** 技能等级: 初级/中级/高级/技师 */
    private String skillLevel;

    /** 技能标签(JSON数组) */
    private String skills;

    private LocalDate entryDate;

    /** 状态: ACTIVE在职/LEAVE休假/RESIGNED离职 */
    private String status;

    /** 时薪 */
    private BigDecimal hourlyRate;

    /** 计件单价 */
    private BigDecimal pieceRate;

    /** 拣货目标(件/天) */
    private Integer targetPicking;

    /** 上架目标(件/天) */
    private Integer targetPutaway;

    /** 收货目标(件/天) */
    private Integer targetReceiving;

    /** 绑定PDA设备 */
    private String pdaDeviceId;

    /** RFID工卡号 */
    private String rfidCard;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
