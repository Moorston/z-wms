package com.xwms.core.vas.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** VAS物料消耗 */
@Data
@TableName("wms_vas_material")
public class VasMaterial {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private Long orderItemId;

    private String materialSku;
    private String materialName;
    private String materialBarcode;

    /** 计划用量 */
    private BigDecimal planQty;

    /** 实际用量 */
    private BigDecimal actualQty;

    private String unit;
    private BigDecimal unitCost;
    private BigDecimal totalCost;

    /** 物料领用库位 */
    private String locationCode;

    private String pickBy;
    private LocalDateTime pickTime;

    /** 状态: PENDING/PICKED/CONSUMED/RETURNED */
    private String status;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
