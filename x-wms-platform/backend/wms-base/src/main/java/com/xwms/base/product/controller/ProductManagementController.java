package com.xwms.base.product.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.product.entity.*;
import com.xwms.base.product.service.ProductManagementService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 产品档案管理 Controller */
@Tag(name = "产品档案管理", description = "产品分类/产品档案/产品包装/产品条码")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductManagementController {

    private final ProductManagementService productManagementService;

    // ============================================================
    // 产品分类
    // ============================================================

    @Operation(summary = "创建产品分类")
    @PostMapping("/category")
    public Result<ProductCategory> createCategory(@RequestBody ProductCategory category) {
        return Result.success(productManagementService.createCategory(category));
    }

    @Operation(summary = "更新产品分类")
    @PutMapping("/category")
    public Result<ProductCategory> updateCategory(@RequestBody ProductCategory category) {
        return Result.success(productManagementService.updateCategory(category));
    }

    @Operation(summary = "分页查询产品分类")
    @GetMapping("/category")
    public Result<Page<ProductCategory>> pageCategories(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String parentCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                productManagementService.pageCategories(
                        new Page<>(page, size), parentCode, status));
    }

    @Operation(summary = "按父级查询分类")
    @GetMapping("/category/parent/{parentCode}")
    public Result<List<ProductCategory>> getCategoriesByParent(@PathVariable String parentCode) {
        return Result.success(productManagementService.getCategoriesByParent(parentCode));
    }

    @Operation(summary = "按编码查询分类")
    @GetMapping("/category/{code}")
    public Result<ProductCategory> getCategoryByCode(@PathVariable String code) {
        return Result.success(productManagementService.getCategoryByCode(code));
    }

    @Operation(summary = "获取分类树")
    @GetMapping("/category/tree")
    public Result<List<ProductCategory>> getCategoryTree(
            @RequestParam(required = false) String rootCode) {
        return Result.success(productManagementService.getCategoryTree(rootCode));
    }

    // ============================================================
    // 产品档案
    // ============================================================

    @Operation(summary = "创建产品")
    @PostMapping
    public Result<Product> createProduct(@RequestBody Product product) {
        return Result.success(productManagementService.createProduct(product));
    }

    @Operation(summary = "更新产品")
    @PutMapping
    public Result<Product> updateProduct(@RequestBody Product product) {
        return Result.success(productManagementService.updateProduct(product));
    }

    @Operation(summary = "分页查询产品")
    @GetMapping
    public Result<Page<Product>> pageProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(
                productManagementService.pageProducts(
                        new Page<>(page, size), ownerCode, categoryCode, status, keyword));
    }

    @Operation(summary = "按货主查询产品")
    @GetMapping("/owner/{ownerCode}")
    public Result<List<Product>> getProductsByOwner(@PathVariable String ownerCode) {
        return Result.success(productManagementService.getProductsByOwner(ownerCode));
    }

    @Operation(summary = "按分类查询产品")
    @GetMapping("/category/{categoryCode}")
    public Result<List<Product>> getProductsByCategory(@PathVariable String categoryCode) {
        return Result.success(productManagementService.getProductsByCategory(categoryCode));
    }

    @Operation(summary = "按编码查询产品")
    @GetMapping("/{code}")
    public Result<Product> getProductByCode(@PathVariable String code) {
        return Result.success(productManagementService.getProductByCode(code));
    }

    @Operation(summary = "通过条码查询产品")
    @GetMapping("/barcode/{barcode}")
    public Result<Product> getProductByBarcode(@PathVariable String barcode) {
        return Result.success(productManagementService.getProductByBarcode(barcode));
    }

    // ============================================================
    // 产品包装
    // ============================================================

    @Operation(summary = "创建产品包装")
    @PostMapping("/package")
    public Result<ProductPackage> createPackage(@RequestBody ProductPackage pkg) {
        return Result.success(productManagementService.createPackage(pkg));
    }

    @Operation(summary = "更新产品包装")
    @PutMapping("/package")
    public Result<ProductPackage> updatePackage(@RequestBody ProductPackage pkg) {
        return Result.success(productManagementService.updatePackage(pkg));
    }

    @Operation(summary = "分页查询产品包装")
    @GetMapping("/package")
    public Result<Page<ProductPackage>> pagePackages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String packageType,
            @RequestParam(required = false) String status) {
        return Result.success(
                productManagementService.pagePackages(
                        new Page<>(page, size), skuCode, packageType, status));
    }

    @Operation(summary = "按SKU查询包装")
    @GetMapping("/package/sku/{skuCode}")
    public Result<List<ProductPackage>> getPackagesBySku(@PathVariable String skuCode) {
        return Result.success(productManagementService.getPackagesBySku(skuCode));
    }

    @Operation(summary = "按编码查询包装")
    @GetMapping("/package/{code}")
    public Result<ProductPackage> getPackageByCode(@PathVariable String code) {
        return Result.success(productManagementService.getPackageByCode(code));
    }

    @Operation(summary = "获取默认包装")
    @GetMapping("/package/default/{skuCode}")
    public Result<ProductPackage> getDefaultPackage(@PathVariable String skuCode) {
        return Result.success(productManagementService.getDefaultPackage(skuCode));
    }

    // ============================================================
    // 产品条码
    // ============================================================

    @Operation(summary = "创建产品条码")
    @PostMapping("/barcode")
    public Result<ProductBarcode> createBarcode(@RequestBody ProductBarcode barcode) {
        return Result.success(productManagementService.createBarcode(barcode));
    }

    @Operation(summary = "更新产品条码")
    @PutMapping("/barcode")
    public Result<ProductBarcode> updateBarcode(@RequestBody ProductBarcode barcode) {
        return Result.success(productManagementService.updateBarcode(barcode));
    }

    @Operation(summary = "分页查询产品条码")
    @GetMapping("/barcode")
    public Result<Page<ProductBarcode>> pageBarcodes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String barcodeType,
            @RequestParam(required = false) String status) {
        return Result.success(
                productManagementService.pageBarcodes(
                        new Page<>(page, size), skuCode, barcodeType, status));
    }

    @Operation(summary = "按SKU查询条码")
    @GetMapping("/barcode/sku/{skuCode}")
    public Result<List<ProductBarcode>> getBarcodesBySku(@PathVariable String skuCode) {
        return Result.success(productManagementService.getBarcodesBySku(skuCode));
    }

    @Operation(summary = "按条码查询")
    @GetMapping("/barcode/{barcode}")
    public Result<ProductBarcode> getBarcodeByCode(@PathVariable String barcode) {
        return Result.success(productManagementService.getBarcodeByCode(barcode));
    }

    @Operation(summary = "获取主条码")
    @GetMapping("/barcode/primary/{skuCode}")
    public Result<ProductBarcode> getPrimaryBarcode(@PathVariable String skuCode) {
        return Result.success(productManagementService.getPrimaryBarcode(skuCode));
    }
}
