package com.xwms.base.master.controller;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.master.entity.Product;
import com.xwms.base.master.mapper.ProductMapper;
import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;

import lombok.RequiredArgsConstructor;

/** 商品档案Controller SKU级别商品主数据管理 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMapper productMapper;

    @PostMapping
    public Result<Product> create(@RequestBody Product product) {
        productMapper.insert(product);
        return Result.success(product);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productMapper.updateById(product);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<Product>> page(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Product> page =
                productMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Product>()
                                .like(sku != null, Product::getSku, sku)
                                .like(productName != null, Product::getProductName, productName)
                                .eq(category != null, Product::getCategory, category)
                                .eq(ownerCode != null, Product::getOwnerCode, ownerCode)
                                .eq(status != null, Product::getStatus, status)
                                .orderByDesc(Product::getCreatedAt));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    @GetMapping("/{sku}")
    public Result<Product> getBySku(@PathVariable String sku) {
        return Result.success(
                productMapper.selectOne(
                        new LambdaQueryWrapper<Product>().eq(Product::getSku, sku)));
    }
}
