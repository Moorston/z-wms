package com.xwms.core.plugin.industry.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 预售订单 电商平台预售订单管理，支持先付定金后付尾款模式 */
@Data
@TableName("wms_ecommerce_pre_sale_order")
public class PreSaleOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预售单号 */
    private String preSaleNo;

    /** 关联订单号 */
    private String orderNo;

    /** 店铺编码 */
    private String shopCode;

    /** 平台编码: TAOBAO淘宝/JD京东/PDD拼多多/DOUYIN抖音/KUAISHOU快手/OTHER其他 */
    private String platformCode;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 规格 */
    private String spec;

    /** 预售数量 */
    private BigDecimal quantity;

    /** 定金金额 */
    private BigDecimal depositAmount;

    /** 尾款金额 */
    private BigDecimal balanceAmount;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 预售开始时间 */
    private LocalDateTime preSaleStartTime;

    /** 预售结束时间 */
    private LocalDateTime preSaleEndTime;

    /** 尾款支付开始时间 */
    private LocalDateTime balancePayStartTime;

    /** 尾款支付结束时间 */
    private LocalDateTime balancePayEndTime;

    /** 预计发货时间 */
    private LocalDateTime expectedShipTime;

    /** 实际发货时间 */
    private LocalDateTime actualShipTime;

    /** 预售状态: PENDING待开始/ACTIVE进行中/ENDED已结束/CANCELLED已取消/SHIPPED已发货 */
    private String status;

    /** 定金支付状态: UNPAID未支付/PAID已支付/REFUNDED已退款 */
    private String depositStatus;

    /** 尾款支付状态: UNPAID未支付/PAID已支付/REFUNDED已退款 */
    private String balanceStatus;

    /** 发货状态: PENDING待发货/PARTIAL部分发货/SHIPPED已发货 */
    private String shipStatus;

    /** 已发货数量 */
    private BigDecimal shippedQty;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

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
