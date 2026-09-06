package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** ASN编组表 多张退货ASN编组成一组，进行播种分货，提高退货处理效率 */
@Data
@TableName("wms_asn_group")
public class AsnGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 编组号 */
    private String groupNo;

    /** 编组名称 */
    private String groupName;

    /** 编组类型：RETURN退货/INBOUND入库/CROSSDOCK越库 */
    private String groupType;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** ASN数量 */
    private Integer asnCount;

    /** SKU种类数 */
    private Integer skuCount;

    /** 总数量 */
    private BigDecimal totalQty;

    /** SKU重合度（百分比） */
    private BigDecimal skuOverlapRate;

    /** 状态：CREATED已创建/SEEDING播种中/SEEDED播种完成/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 播种模式：STATIC静态播种/DYNAMIC动态播种 */
    private String sowingMode;

    /** 播种位数量 */
    private Integer sowingLocationCount;

    /** 播种开始时间 */
    private LocalDateTime sowingStartTime;

    /** 播种完成时间 */
    private LocalDateTime sowingFinishTime;

    /** 操作人 */
    private String operator;

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
