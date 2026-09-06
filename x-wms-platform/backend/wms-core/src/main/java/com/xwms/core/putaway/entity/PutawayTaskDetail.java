package com.xwms.core.putaway.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 上架任务明细表 */
@Data
@TableName("wms_putaway_task_detail")
public class PutawayTaskDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 明细号 */
    private String detailNo;

    /** 上架任务号 */
    private String taskNo;

    /** 行号 */
    private Integer lineNo;

    /** 关联入库明细号 */
    private String inboundDetailNo;

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

    /** 包装数量 */
    private BigDecimal packageQty;

    /** 商品重量（kg） */
    private BigDecimal productWeight;

    /** 商品高度（cm） */
    private BigDecimal productHeight;

    /** 预期数量 */
    private BigDecimal expectedQty;

    /** 已上架数量 */
    private BigDecimal putawayQty;

    /** 差异数量 */
    private BigDecimal differenceQty;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 序列号 */
    private String serialNo;

    /** 源库位（收货库位） */
    private String sourceLocation;

    /** 推荐目标库位 */
    private String recommendLocation;

    /** 实际目标库位 */
    private String actualLocation;

    /** 推荐库区 */
    private String recommendArea;

    /** 实际库区 */
    private String actualArea;

    /** 库位类型：STORAGE存储/PICKING拣货/RECEIVING收货/SHIPPING发货/BULK大宗 */
    private String locationType;

    /** 是否需要批次管理：Y/N */
    private String batchManaged;

    /** 是否需要序列号管理：Y/N */
    private String serialManaged;

    /** 是否需要效期管理：Y/N */
    private String expiryManaged;

    /** 状态：PENDING/PUTAWAYING/PARTIAL/COMPLETED/CANCELLED */
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
