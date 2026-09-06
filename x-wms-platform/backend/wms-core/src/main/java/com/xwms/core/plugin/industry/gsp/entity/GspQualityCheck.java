package com.xwms.core.plugin.industry.gsp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** GSP质检记录 药品经营质量管理规范(GSP)入库验收、在库养护、出库复核记录 */
@Data
@TableName("wms_gsp_quality_check")
public class GspQualityCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质检单号 */
    private String checkNo;

    /**
     * 质检类型:
     * INBOUND_ACCEPTANCE入库验收/STOCK_MAINTENANCE在库养护/OUTBOUND_REVIEW出库复核/RETURN_CHECK退货验收/SAMPLING抽样检查
     */
    private String checkType;

    /** 关联业务单号 */
    private String refNo;

    /** 批次号 */
    private String batchNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 规格 */
    private String spec;

    /** 生产厂家 */
    private String manufacturer;

    /** 批准文号 */
    private String approvalNumber;

    /** 质检数量 */
    private BigDecimal checkQty;

    /** 抽样数量 */
    private BigDecimal sampleQty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 不合格数量 */
    private BigDecimal unqualifiedQty;

    /** 外观检查: QUALIFIED合格/UNQUALIFIED不合格 */
    private String appearanceCheck;

    /** 包装检查: QUALIFIED合格/UNQUALIFIED不合格 */
    private String packagingCheck;

    /** 标签检查: QUALIFIED合格/UNQUALIFIED不合格 */
    private String labelCheck;

    /** 有效期检查: QUALIFIED合格/UNQUALIFIED不合格 */
    private String expiryCheck;

    /** 批号检查: QUALIFIED合格/UNQUALIFIED不合格 */
    private String batchCheck;

    /** 温度记录: NORMAL正常/ABNORMAL异常 */
    private String tempRecord;

    /** 质检结果: QUALIFIED合格/UNQUALIFIED不合格/PARTIAL部分合格 */
    private String checkResult;

    /** 不合格原因 */
    private String unqualifiedReason;

    /** 处理方式: RETURN退货/DESTROY销毁/REWORK返工/ACCEPT让步接收 */
    private String handleMethod;

    /** 处理意见 */
    private String handleOpinion;

    /** 质检状态: PENDING待质检/CHECKING质检中/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 质检人 */
    private String checker;

    /** 质检时间 */
    private LocalDateTime checkTime;

    /** 复核人 */
    private String reviewer;

    /** 复核时间 */
    private LocalDateTime reviewTime;

    /** 批准人 */
    private String approver;

    /** 批准时间 */
    private LocalDateTime approveTime;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库区编码 */
    private String areaCode;

    /** 库位编码 */
    private String locationCode;

    /** 货主编码 */
    private String ownerCode;

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
