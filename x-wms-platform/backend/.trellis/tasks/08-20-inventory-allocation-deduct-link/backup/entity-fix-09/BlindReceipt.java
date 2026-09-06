package com.xwms.core.receipt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盲收记录表
 * 无单据收货，收货后反向生成ASN明细
 * 状态流转：BLIND_RECEIVED盲收完成→MATCHED已匹配→ASN_CREATED已生成ASN→CANCELLED已取消
 */
@Data
@TableName("wms_blind_receipt")
public class BlindReceipt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 盲收单号 */
    private String blindNo;

    /** 盲收模式：NORMAL普通模式/SIMPLIFIED简化模式 */
    private String blindMode;

    /** 供应商编码（可选，盲收时可能未知） */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 收货库区 */
    private String receiveArea;

    /** 收货库位 */
    private String receiveLocation;

    /** 月台号 */
    private String dockNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 商品条码 */
    private String barcode;

    /** 规格型号 */
    private String spec;

    /** 单位 */
    private String unit;

    /** 盲收数量 */
    private BigDecimal blindQty;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 序列号 */
    private String serialNo;

    /** 箱号/LPN号 */
    private String lpnNo;

    /** 状态：BLIND_RECEIVED/MATCHED/ASN_CREATED/CANCELLED */
    private String status;

    /** 匹配的PO号 */
    private String matchedPoNo;

    /** 匹配的ASN号 */
    private String matchedAsnNo;

    /** 匹配的入库单号 */
    private String matchedInboundNo;

    /** 匹配时间 */
    private LocalDateTime matchTime;

    /** 匹配人 */
    private String matchedBy;

    /** 生成ASN时间 */
    private LocalDateTime asnCreateTime;

    /** 车牌号 */
    private String vehicleNo;

    /** 司机姓名 */
    private String driverName;

    /** 收货人 */
    private String operator;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 设备号 */
    private String deviceNo;

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
