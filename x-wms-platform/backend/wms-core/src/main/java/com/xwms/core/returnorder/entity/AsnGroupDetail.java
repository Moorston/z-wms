package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** ASN编组明细表 记录编组中包含的ASN明细及播种分货信息 */
@Data
@TableName("wms_asn_group_detail")
public class AsnGroupDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 编组号 */
    private String groupNo;

    /** ASN号 */
    private String asnNo;

    /** 退货单号 */
    private String returnNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 数量 */
    private BigDecimal qty;

    /** 已播种数量 */
    private BigDecimal sowedQty;

    /** 播种位编码 */
    private String sowingLocation;

    /** 播种次序（动态播种时使用） */
    private Integer sowingSeq;

    /** 状态：PENDING待播种/SEEDED已播种/COMPLETED已完成 */
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
