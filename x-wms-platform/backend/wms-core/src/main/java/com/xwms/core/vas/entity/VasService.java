package com.xwms.core.vas.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** VAS服务定义 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_vas_service")
public class VasService extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String serviceCode;
    private String serviceName;

    /** 服务类型: LABELING/REPACK/KITTING/SPLIT/ASSEMBLY/INSPECTION/CUSTOM/OTHER */
    private String serviceType;

    /** 服务分类: PACKAGING/LABELING/PROCESSING/QUALITY/OTHER */
    private String category;

    private String description;

    /** 计费单位: PIECE/ORDER/LOT/HOUR/KG */
    private String unit;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 标准工时(分钟/单位) */
    private BigDecimal standardTime;

    /** 是否需要物料 */
    private Integer needMaterial;

    /** 所需物料清单(JSON) */
    private String materialList;

    /** 是否需要设备 */
    private Integer needEquipment;

    /** 所需设备清单(JSON) */
    private String equipmentList;

    /** 所需技能 */
    private String skillRequired;

    /** 状态: ENABLED/DISABLED */
    private String status;

    private String ownerCode;
    private String warehouseCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
