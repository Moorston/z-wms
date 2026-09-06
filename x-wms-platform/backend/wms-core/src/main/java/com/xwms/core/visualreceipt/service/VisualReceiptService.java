package com.xwms.core.visualreceipt.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.config.service.SysConfigService;
import com.xwms.core.visualreceipt.entity.ProductImage;
import com.xwms.core.visualreceipt.mapper.ProductImageMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 可视化收货服务 适用于缺少条码，依据描述难以明确区分产品的收货情况，根据图片辅助识别产品 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisualReceiptService {

    private final ProductImageMapper productImageMapper;
    private final SysConfigService sysConfigService;

    /** 上传产品图片 */
    @Transactional(rollbackFor = Exception.class)
    public ProductImage uploadImage(
            String skuCode, String skuName, String imageUrl, String imageType, String operator) {
        log.info("上传产品图片: sku={}, type={}", skuCode, imageType);
        ProductImage image = new ProductImage();
        image.setImageCode("IMG" + System.currentTimeMillis());
        image.setSkuCode(skuCode);
        image.setSkuName(skuName);
        image.setImageUrl(imageUrl);
        image.setImageType(imageType != null ? imageType : "MAIN");
        image.setSortOrder(0);
        image.setEnabled("Y");
        image.setCreatedBy(operator);
        image.setCreatedTime(LocalDateTime.now());
        productImageMapper.insert(image);
        return image;
    }

    /** 根据SKU查询产品图片列表（可视化收货使用） */
    public List<ProductImage> getImagesBySku(String skuCode) {
        return productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getSkuCode, skuCode)
                        .eq(ProductImage::getEnabled, "Y")
                        .orderByAsc(ProductImage::getSortOrder));
    }

    /** 可视化收货：根据ASN查询待收货产品图片列表 选择ASN明细→右键→可视化收货，系统显示产品图片列表 */
    public List<ProductImage> getVisualReceiptImages(String asnNo) {
        log.info("可视化收货查询图片: asnNo={}", asnNo);
        // TODO: 从ASN明细获取SKU列表，然后查询每个SKU的图片
        // 这里简化处理，实际需要关联ASN明细表
        return productImageMapper.selectList(
                new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getEnabled, "Y")
                        .orderByAsc(ProductImage::getSortOrder)
                        .last("LIMIT 50"));
    }

    /** 检查是否开启快捷收货显示产品图片（RCV_SHW_PIC参数） */
    public boolean isShowProductImage() {
        return sysConfigService.getBooleanConfig(SysConfigService.RCV_SHW_PIC);
    }

    /** 删除产品图片 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(String imageCode, String operator) {
        ProductImage image = productImageMapper.selectByCode(imageCode);
        if (image != null) {
            image.setEnabled("N");
            image.setUpdatedBy(operator);
            image.setUpdatedTime(LocalDateTime.now());
            productImageMapper.updateById(image);
        }
    }
}
