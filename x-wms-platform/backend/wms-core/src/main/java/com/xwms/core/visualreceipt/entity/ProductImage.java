package com.xwms.core.visualreceipt.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 产品图片表 用于可视化收货，根据图片辅助识别产品 */
@Data
@TableName("wms_product_image")
public class ProductImage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String imageCode;
    private String skuCode;
    private String skuName;
    private String imageUrl;
    private String imageType; // MAIN主图/DETAIL细节图/PACKAGE包装图
    private Integer sortOrder;
    private String imageSize; // 尺寸配置
    private String enabled;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
}
