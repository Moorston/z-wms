package com.xwms.core.putaway.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架记录表 记录每次实际上架操作 */
@Data
@TableName("wms_putaway_record")
public class PutawayRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上架记录号 */
    private String recordNo;

    /** 上架任务号 */
    private String taskNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联ASN号 */
    private String asnNo;

    /** 上架任务明细号 */
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

    /** 上架数量 */
    private BigDecimal putawayQty;

    /** 单位 */
    private String unit;

    /** 包装代码 */
    private String packageCode;

    /** 包装数量 */
    private BigDecimal packageQty;

    /** 托盘号/LPN号 */
    private String lpnNo;

    /** 源库位（收货库位） */
    private String sourceLocation;

    /** 推荐目标库位 */
    private String recommendLocation;

    /** 实际目标库位 */
    private String targetLocation;

    /** 目标库区 */
    private String targetArea;

    /** 库位类型 */
    private String locationType;

    /** 上架方式：STANDARD/QUICK/MERGE/BATCH/LPN/DIRECT/RESERVATION */
    private String putawayType;

    /** 上架策略 */
    private String putawayStrategy;

    /** 是否使用系统推荐库位：Y/N */
    private String useSystemRecommend;

    /** 差异数量（上架数量-预期数量） */
    private BigDecimal differenceQty;

    /** 差异类型：OVER多收/SHORT少收/NONE无差异 */
    private String differenceType;

    /** 上架人 */
    private String operator;

    /** 上架时间 */
    private LocalDateTime putawayTime;

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
