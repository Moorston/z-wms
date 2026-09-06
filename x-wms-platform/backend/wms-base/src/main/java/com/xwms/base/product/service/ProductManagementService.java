package com.xwms.base.product.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.product.entity.*;
import com.xwms.base.product.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 产品档案管理核心服务 包含: 产品分类/产品档案/产品包装/产品条码 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductManagementService {

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;
    private final ProductPackageMapper productPackageMapper;
    private final ProductBarcodeMapper productBarcodeMapper;

    // ============================================================
    // 1. 产品分类管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ProductCategory createCategory(ProductCategory category) {
        if (category.getStatus() == null) category.setStatus("ACTIVE");
        if (category.getCategoryLevel() == null) category.setCategoryLevel(1);
        productCategoryMapper.insert(category);
        log.info("创建产品分类: {}={}", category.getCategoryCode(), category.getCategoryName());
        return category;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductCategory updateCategory(ProductCategory category) {
        productCategoryMapper.updateById(category);
        return category;
    }

    public Page<ProductCategory> pageCategories(
            Page<ProductCategory> page, String parentCode, String status) {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (parentCode != null) wrapper.eq(ProductCategory::getParentCode, parentCode);
        if (status != null) wrapper.eq(ProductCategory::getStatus, status);
        wrapper.orderByAsc(ProductCategory::getCategoryLevel)
                .orderByAsc(ProductCategory::getSortOrder);
        return productCategoryMapper.selectPage(page, wrapper);
    }

    public List<ProductCategory> getCategoriesByParent(String parentCode) {
        return productCategoryMapper.selectByParent(parentCode);
    }

    public ProductCategory getCategoryByCode(String categoryCode) {
        return productCategoryMapper.selectByCode(categoryCode);
    }

    /** 获取分类树（递归） */
    public List<ProductCategory> getCategoryTree(String rootCode) {
        List<ProductCategory> roots = productCategoryMapper.selectByParent(rootCode);
        for (ProductCategory root : roots) {
            root.setCategoryName(root.getCategoryName());
            // 递归获取子分类（简化实现，实际可用递归或CTE）
        }
        return roots;
    }

    // ============================================================
    // 2. 产品档案管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Product createProduct(Product product) {
        if (product.getStatus() == null) product.setStatus("ACTIVE");
        if (product.getIsBatchMgmt() == null) product.setIsBatchMgmt(0);
        if (product.getIsSerialMgmt() == null) product.setIsSerialMgmt(0);
        if (product.getIsFragile() == null) product.setIsFragile(0);
        if (product.getIsHazardous() == null) product.setIsHazardous(0);
        productMapper.insert(product);
        log.info("创建产品: {}={}", product.getSkuCode(), product.getSkuName());
        return product;
    }

    @Transactional(rollbackFor = Exception.class)
    public Product updateProduct(Product product) {
        productMapper.updateById(product);
        return product;
    }

    public Page<Product> pageProducts(
            Page<Product> page,
            String ownerCode,
            String categoryCode,
            String status,
            String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (ownerCode != null) wrapper.eq(Product::getOwnerCodeCol, ownerCode);
        if (categoryCode != null) wrapper.eq(Product::getCategoryCode, categoryCode);
        if (status != null) wrapper.eq(Product::getStatus, status);
        if (keyword != null)
            wrapper.and(
                    w ->
                            w.like(Product::getSkuCode, keyword)
                                    .or()
                                    .like(Product::getSkuName, keyword)
                                    .or()
                                    .like(Product::getSkuShortName, keyword));
        wrapper.orderByAsc(Product::getSkuCode);
        return productMapper.selectPage(page, wrapper);
    }

    public List<Product> getProductsByOwner(String ownerCode) {
        return productMapper.selectByOwner(ownerCode);
    }

    public List<Product> getProductsByCategory(String categoryCode) {
        return productMapper.selectByCategory(categoryCode);
    }

    public Product getProductByCode(String skuCode) {
        return productMapper.selectByCode(skuCode);
    }

    /** 通过条码查询产品 */
    public Product getProductByBarcode(String barcode) {
        ProductBarcode productBarcode = productBarcodeMapper.selectByBarcode(barcode);
        if (productBarcode == null) return null;
        return productMapper.selectByCode(productBarcode.getSkuCode());
    }

    // ============================================================
    // 3. 产品包装管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ProductPackage createPackage(ProductPackage pkg) {
        if (pkg.getStatus() == null) pkg.setStatus("ACTIVE");
        if (pkg.getIsDefault() == null) pkg.setIsDefault(0);
        // 如果是默认包装，取消其他默认包装
        if (pkg.getIsDefault() == 1) {
            cancelDefaultPackage(pkg.getSkuCode());
        }
        productPackageMapper.insert(pkg);
        log.info("创建产品包装: {}={}", pkg.getPackageCode(), pkg.getPackageName());
        return pkg;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductPackage updatePackage(ProductPackage pkg) {
        if (pkg.getIsDefault() != null && pkg.getIsDefault() == 1) {
            cancelDefaultPackage(pkg.getSkuCode());
        }
        productPackageMapper.updateById(pkg);
        return pkg;
    }

    public Page<ProductPackage> pagePackages(
            Page<ProductPackage> page, String skuCode, String packageType, String status) {
        LambdaQueryWrapper<ProductPackage> wrapper = new LambdaQueryWrapper<>();
        if (skuCode != null) wrapper.eq(ProductPackage::getSkuCode, skuCode);
        if (packageType != null) wrapper.eq(ProductPackage::getPackageType, packageType);
        if (status != null) wrapper.eq(ProductPackage::getStatus, status);
        wrapper.orderByDesc(ProductPackage::getIsDefault)
                .orderByAsc(ProductPackage::getPackageCode);
        return productPackageMapper.selectPage(page, wrapper);
    }

    public List<ProductPackage> getPackagesBySku(String skuCode) {
        return productPackageMapper.selectBySku(skuCode);
    }

    public ProductPackage getPackageByCode(String packageCode) {
        return productPackageMapper.selectByCode(packageCode);
    }

    /** 获取默认包装 */
    public ProductPackage getDefaultPackage(String skuCode) {
        return productPackageMapper.selectOne(
                new LambdaQueryWrapper<ProductPackage>()
                        .eq(ProductPackage::getSkuCode, skuCode)
                        .eq(ProductPackage::getIsDefault, 1)
                        .last("LIMIT 1"));
    }

    private void cancelDefaultPackage(String skuCode) {
        List<ProductPackage> defaults =
                productPackageMapper.selectList(
                        new LambdaQueryWrapper<ProductPackage>()
                                .eq(ProductPackage::getSkuCode, skuCode)
                                .eq(ProductPackage::getIsDefault, 1));
        for (ProductPackage p : defaults) {
            p.setIsDefault(0);
            productPackageMapper.updateById(p);
        }
    }

    // ============================================================
    // 4. 产品条码管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ProductBarcode createBarcode(ProductBarcode barcode) {
        if (barcode.getStatus() == null) barcode.setStatus("ACTIVE");
        if (barcode.getIsPrimary() == null) barcode.setIsPrimary(0);
        // 如果是主条码，取消其他主条码
        if (barcode.getIsPrimary() == 1) {
            cancelPrimaryBarcode(barcode.getSkuCode());
        }
        productBarcodeMapper.insert(barcode);
        log.info("创建产品条码: {} -> {}", barcode.getBarcode(), barcode.getSkuCode());
        return barcode;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductBarcode updateBarcode(ProductBarcode barcode) {
        if (barcode.getIsPrimary() != null && barcode.getIsPrimary() == 1) {
            cancelPrimaryBarcode(barcode.getSkuCode());
        }
        productBarcodeMapper.updateById(barcode);
        return barcode;
    }

    public Page<ProductBarcode> pageBarcodes(
            Page<ProductBarcode> page, String skuCode, String barcodeType, String status) {
        LambdaQueryWrapper<ProductBarcode> wrapper = new LambdaQueryWrapper<>();
        if (skuCode != null) wrapper.eq(ProductBarcode::getSkuCode, skuCode);
        if (barcodeType != null) wrapper.eq(ProductBarcode::getBarcodeType, barcodeType);
        if (status != null) wrapper.eq(ProductBarcode::getStatus, status);
        wrapper.orderByDesc(ProductBarcode::getIsPrimary).orderByAsc(ProductBarcode::getBarcode);
        return productBarcodeMapper.selectPage(page, wrapper);
    }

    public List<ProductBarcode> getBarcodesBySku(String skuCode) {
        return productBarcodeMapper.selectBySku(skuCode);
    }

    public ProductBarcode getBarcodeByCode(String barcode) {
        return productBarcodeMapper.selectByBarcode(barcode);
    }

    /** 获取主条码 */
    public ProductBarcode getPrimaryBarcode(String skuCode) {
        return productBarcodeMapper.selectOne(
                new LambdaQueryWrapper<ProductBarcode>()
                        .eq(ProductBarcode::getSkuCode, skuCode)
                        .eq(ProductBarcode::getIsPrimary, 1)
                        .last("LIMIT 1"));
    }

    private void cancelPrimaryBarcode(String skuCode) {
        List<ProductBarcode> primaries =
                productBarcodeMapper.selectList(
                        new LambdaQueryWrapper<ProductBarcode>()
                                .eq(ProductBarcode::getSkuCode, skuCode)
                                .eq(ProductBarcode::getIsPrimary, 1));
        for (ProductBarcode b : primaries) {
            b.setIsPrimary(0);
            productBarcodeMapper.updateById(b);
        }
    }
}
