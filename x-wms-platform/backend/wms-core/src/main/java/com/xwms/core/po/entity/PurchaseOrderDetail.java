package com.xwms.core.po.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 采购订单明细表 记录PO中的商品明细行 */
@Data
@TableName("wms_purchase_order_detail")
public class PurchaseOrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 明细号 */
    private String detailNo;

    /** 采购订单号 */
    private String poNo;

    /** 行号 */
    private Integer lineNo;

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

    /** 包装代码 */
    private String packageCode;

    /** 包装数量（每包装件数） */
    private BigDecimal packageQty;

    /** 订购数量 */
    private BigDecimal orderQty;

    /** 已释放数量（提取到ASN的数量） */
    private BigDecimal releasedQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已入库数量 */
    private BigDecimal putawayQty;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 金额 */
    private BigDecimal amount;

    /** 税率 */
    private BigDecimal taxRate;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 是否需要批次管理：Y/N */
    private String batchManaged;

    /** 是否需要序列号管理：Y/N */
    private String serialManaged;

    /** 是否需要效期管理：Y/N */
    private String expiryManaged;

    /** 是否需要质检：Y/N */
    private String qcRequired;

    /** 质检类型：FULL全检/SAMPLE抽检/NONE不检 */
    private String qcType;

    /** 默认收货库位 */
    private String defaultReceiveLocation;

    /** 默认上架库区 */
    private String defaultPutawayArea;

    /** 状态：CREATED/RELEASED/PARTIAL_RECEIVED/FULLY_RECEIVED/COMPLETED/CANCELLED */
    private String status;

    /** 关闭原因 */
    private String closeReason;

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
