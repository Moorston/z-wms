package com.xwms.core.crossdock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 越库预分表 RF扫描箱码/箱序列号，系统按箱号执行收货确认，后台对已收货库存进行分配，提取订单播种位显示 */
@Data
@TableName("wms_crossdock_pre_sort")
public class CrossdockPreSort {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预分单号 */
    private String sortNo;

    /** 越库单号 */
    private String crossdockNo;

    /** 预配单号 */
    private String allocNo;

    /** ASN号 */
    private String asnNo;

    /** 箱号/LPN */
    private String boxNo;

    /** 箱序列号 */
    private String boxSerialNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 箱内数量 */
    private BigDecimal boxQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已分拣数量 */
    private BigDecimal sortedQty;

    /** 出库单号 */
    private String outboundNo;

    /** 播种位编码 */
    private String sowingLocation;

    /** 出库月台 */
    private String outboundDock;

    /** 类型：XDOCK需越库/NONE_XDOCK无需越库正常入库 */
    private String xdockType;

    /** 状态：PENDING待扫描/SCANNED已扫描/RECEIVED已收货/ALLOCATED已分配/SORTED已分拣/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 扫描人 */
    private String scanner;

    /** 扫描时间 */
    private LocalDateTime scanTime;

    /** 收货时间 */
    private LocalDateTime receiveTime;

    /** 分拣时间 */
    private LocalDateTime sortTime;

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
