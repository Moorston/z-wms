package com.xwms.core.visualreceipt.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.visualreceipt.entity.ProductImage;
import com.xwms.core.visualreceipt.service.VisualReceiptService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/visual-receipt")
@RequiredArgsConstructor
public class VisualReceiptController {

    private final VisualReceiptService visualReceiptService;

    @PostMapping("/image/upload")
    public ProductImage uploadImage(
            @RequestParam String skuCode,
            @RequestParam String skuName,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String imageType,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return visualReceiptService.uploadImage(skuCode, skuName, imageUrl, imageType, operator);
    }

    @GetMapping("/image/sku/{skuCode}")
    public List<ProductImage> getImagesBySku(@PathVariable String skuCode) {
        return visualReceiptService.getImagesBySku(skuCode);
    }

    @GetMapping("/images/{asnNo}")
    public List<ProductImage> getVisualReceiptImages(@PathVariable String asnNo) {
        return visualReceiptService.getVisualReceiptImages(asnNo);
    }

    @GetMapping("/show-image")
    public Map<String, Object> isShowProductImage() {
        return Map.of("showImage", visualReceiptService.isShowProductImage());
    }

    @DeleteMapping("/image/{imageCode}")
    public Map<String, Object> deleteImage(
            @PathVariable String imageCode,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        visualReceiptService.deleteImage(imageCode, operator);
        return Map.of("success", true, "message", "图片已删除");
    }
}
