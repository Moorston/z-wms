package com.xwms.base.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.product.entity.ProductPackage;

@Mapper
public interface ProductPackageMapper extends BaseMapper<ProductPackage> {

    @Select(
            "SELECT * FROM wms_product_package WHERE sku_code = #{skuCode} AND status = 'ACTIVE' ORDER BY is_default DESC, package_code")
    List<ProductPackage> selectBySku(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM wms_product_package WHERE package_code = #{code}")
    ProductPackage selectByCode(@Param("code") String code);
}
