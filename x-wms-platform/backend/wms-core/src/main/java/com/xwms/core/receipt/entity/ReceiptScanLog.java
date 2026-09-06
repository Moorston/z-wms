package com.xwms.core.receipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 扫描收货日志表 记录每次扫描操作，支持批量/逐件/逐箱/序列号模式 */
@Data
@TableName("wms_receipt_scan_log")
public class ReceiptScanLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 扫描日志号 */
    private String scanNo;

    /** 收货任务号 */
    private String taskNo;

    /** 收货记录号 */
    private String recordNo;

    /** 扫描类型：BARCODE条码/SERIAL序列号/LPN箱号/BATCH批次 */
    private String scanType;

    /** 扫描内容（条码/序列号/箱号/批次号） */
    private String scanContent;

    /** 商品编码（扫描后匹配） */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 序列号 */
    private String serialNo;

    /** 箱号/LPN号 */
    private String lpnNo;

    /** 扫描数量 */
    private BigDecimal scanQty;

    /** 扫描模式：BATCH批量/PIECE逐件/BOX逐箱/SERIAL序列号 */
    private String scanMode;

    /** 扫描结果：SUCCESS成功/FAIL失败/DUPLICATE重复/UNKNOWN未知 */
    private String scanResult;

    /** 失败原因 */
    private String failReason;

    /** 是否满箱提醒：Y/N */
    private String fullBoxAlert;

    /** 扫描库位 */
    private String scanLocation;

    /** 扫描人 */
    private String operator;

    /** 扫描时间 */
    private LocalDateTime scanTime;

    /** 设备号（PDA/RF） */
    private String deviceNo;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
