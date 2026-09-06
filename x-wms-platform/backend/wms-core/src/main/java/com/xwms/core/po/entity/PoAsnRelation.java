package com.xwms.core.po.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** PO-ASN关联表 记录从PO提取生成ASN的明细关系 支持一个ASN从多个PO提取，一个PO关联多个ASN */
@Data
@TableName("wms_po_asn_relation")
public class PoAsnRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联号 */
    private String relationNo;

    /** 采购订单号 */
    private String poNo;

    /** PO行号 */
    private Integer poLineNo;

    /** PO明细号 */
    private String poDetailNo;

    /** ASN编号 */
    private String asnNo;

    /** ASN行号 */
    private Integer asnLineNo;

    /** 商品编码 */
    private String skuCode;

    /** 提取数量（从PO释放到ASN的数量） */
    private BigDecimal releaseQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已入库数量 */
    private BigDecimal putawayQty;

    /** 提取时间 */
    private LocalDateTime releaseTime;

    /** 提取人 */
    private String releasedBy;

    /** 状态：RELEASED已释放/RECEIVED已收货/PUTAWAY已入库/CANCELLED已取消 */
    private String status;

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
