package com.xwms.core.receipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 收货记录表 记录每次实际收货操作 */
@Data
@TableName("wms_receipt_record")
public class ReceiptRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收货记录号 */
    private String recordNo;

    /** 收货任务号 */
    private String taskNo;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 收货任务明细号 */
    private String taskDetailNo;

    /** 关联入库明细号 */
    private String inboundDetailNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 商品条码 */
    private String barcode;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 序列号 */
    private String serialNo;

    /** 收货数量 */
    private BigDecimal receiveQty;

    /** 收货单位 */
    private String unit;

    /** 包装代码 */
    private String packageCode;

    /** 包装数量 */
    private BigDecimal packageQty;

    /** 箱号/LPN号 */
    private String lpnNo;

    /** 收货库位 */
    private String receiveLocation;

    /** 收货库区 */
    private String receiveArea;

    /** 月台号 */
    private String dockNo;

    /** 收货方式：ASN/PARTIAL/PALLET/SCAN/BOX/QUICK/VISUAL/MIX/COMPONENT/SORT/PRE/BLIND */
    private String receiptType;

    /** 扫描模式：BATCH/PIECE/BOX/SERIAL */
    private String scanMode;

    /** 差异数量（收货数量-预期数量） */
    private BigDecimal differenceQty;

    /** 差异类型：OVER多收/SHORT少收/NONE无差异 */
    private String differenceType;

    /** 是否需要质检：Y/N */
    private String qcRequired;

    /** 质检状态 */
    private String qcStatus;

    /** 是否直接上架：Y/N */
    private String directPutaway;

    /** 收货人 */
    private String operator;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 设备号（PDA/RF） */
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
