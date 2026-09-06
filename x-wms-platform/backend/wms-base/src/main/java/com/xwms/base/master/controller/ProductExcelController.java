package com.xwms.base.master.controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.xwms.base.master.entity.Product;
import com.xwms.base.master.mapper.ProductMapper;
import com.xwms.base.master.vo.ProductExcelVO;
import com.xwms.common.core.Result;
import com.xwms.common.excel.dto.ImportResult;
import com.xwms.common.excel.util.ExcelUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 商品导入导出Controller 提供商品档案的批量导入、导出、模板下载功能 */
@Slf4j
@RestController
@RequestMapping("/api/product/excel")
@RequiredArgsConstructor
public class ProductExcelController {

    private final ProductMapper productMapper;

    /** 下载导入模板 */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        ExcelUtil.downloadTemplate(response, "商品导入模板", ProductExcelVO.class);
    }

    /** 批量导入商品 */
    @PostMapping("/import")
    public Result<ImportResult<ProductExcelVO>> importProducts(
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("开始导入商品: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        ImportResult<ProductExcelVO> result =
                ExcelUtil.importBatch(
                        file,
                        ProductExcelVO.class,
                        100,
                        // 校验逻辑
                        vo -> {
                            if (vo.getSku() == null || vo.getSku().isEmpty()) {
                                return "SKU编码不能为空";
                            }
                            if (vo.getProductName() == null || vo.getProductName().isEmpty()) {
                                return "商品名称不能为空";
                            }
                            return null;
                        },
                        // 批量保存逻辑
                        batch -> {
                            List<Product> products =
                                    batch.stream()
                                            .map(this::convertToEntity)
                                            .collect(Collectors.toList());
                            products.forEach(productMapper::insert);
                        });

        log.info(
                "商品导入完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailCount(),
                result.getCostMillis());

        return Result.success(result);
    }

    /** 导出商品 */
    @GetMapping("/export")
    public void exportProducts(
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String category,
            HttpServletResponse response)
            throws IOException {
        // 查询数据
        List<Product> products =
                productMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                                        Product>()
                                .eq(ownerCode != null, Product::getOwnerCode, ownerCode)
                                .eq(category != null, Product::getCategory, category)
                                .orderByDesc(Product::getCreatedAt));

        // 转换为VO
        List<ProductExcelVO> voList =
                products.stream().map(this::convertToVO).collect(Collectors.toList());

        // 导出
        ExcelUtil.export(response, "商品档案", "商品列表", ProductExcelVO.class, voList);
    }

    /** VO转Entity */
    private Product convertToEntity(ProductExcelVO vo) {
        Product product = new Product();
        product.setSku(vo.getSku());
        product.setProductName(vo.getProductName());
        product.setOwnerCode(vo.getOwnerCode());
        product.setBarcode(vo.getBarcode());
        product.setCategory(vo.getCategory());
        product.setBrand(vo.getBrand());
        product.setSpec(vo.getSpec());
        product.setUnit(vo.getUnit());
        product.setWeight(vo.getWeight());
        product.setVolume(vo.getVolume());
        product.setTemperatureZone(vo.getTemperatureZone());
        product.setShelfLifeDays(vo.getShelfLifeDays());
        product.setBatchManaged("是".equals(vo.getBatchManaged()));
        product.setSerialManaged("是".equals(vo.getSerialManaged()));
        product.setStatus("停用".equals(vo.getStatus()) ? "DISABLED" : "ACTIVE");
        return product;
    }

    /** Entity转VO */
    private ProductExcelVO convertToVO(Product product) {
        ProductExcelVO vo = new ProductExcelVO();
        vo.setSku(product.getSku());
        vo.setProductName(product.getProductName());
        vo.setOwnerCode(product.getOwnerCode());
        vo.setBarcode(product.getBarcode());
        vo.setCategory(product.getCategory());
        vo.setBrand(product.getBrand());
        vo.setSpec(product.getSpec());
        vo.setUnit(product.getUnit());
        vo.setWeight(product.getWeight());
        vo.setVolume(product.getVolume());
        vo.setTemperatureZone(product.getTemperatureZone());
        vo.setShelfLifeDays(product.getShelfLifeDays());
        vo.setBatchManaged(Boolean.TRUE.equals(product.getBatchManaged()) ? "是" : "否");
        vo.setSerialManaged(Boolean.TRUE.equals(product.getSerialManaged()) ? "是" : "否");
        vo.setStatus("DISABLED".equals(product.getStatus()) ? "停用" : "启用");
        return vo;
    }
}
