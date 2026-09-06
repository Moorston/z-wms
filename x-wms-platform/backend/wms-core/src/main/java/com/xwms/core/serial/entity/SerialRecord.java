package com.xwms.core.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 序列号采集记录表 记录入库/出库/退货等业务场景的序列号采集过程 */
@Data
@TableName("wms_serial_record")
public class SerialRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采集单号 */
    private String recordNo;

    /** 业务类型：INBOUND入库/OUTBOUND出库/RETURN退货/TRANSFER调拨/ADJUST调整 */
    private String businessType;

    /** 关联单据号（入库单/出库单/退货单等） */
    private String refNo;

    /** 关联单据明细号 */
    private String refDetailNo;

    /** ASN号 */
    private String asnNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 批次号 */
    private String batchNo;

    /** 序列号（多个用逗号分隔，或关联明细表） */
    private String serialNos;

    /** 采集数量 */
    private Integer collectQty;

    /** 应采集数量 */
    private Integer expectedQty;

    /** 采集状态：PENDING待采集/COLLECTING采集中/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 采集模式：SCAN扫描/IMPORT导入/MANUAL手工 */
    private String collectMode;

    /** 采集人 */
    private String collector;

    /** 采集开始时间 */
    private LocalDateTime startTime;

    /** 采集完成时间 */
    private LocalDateTime finishTime;

    /** 库位编码 */
    private String locationCode;

    /** 托盘号/LPN */
    private String lpnNo;

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
