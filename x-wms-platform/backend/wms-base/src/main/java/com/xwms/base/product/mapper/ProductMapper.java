package com.xwms.base.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.product.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select(
            "SELECT * FROM wms_product WHERE owner_code_col = #{ownerCode} AND status = 'ACTIVE' ORDER BY sku_code")
    List<Product> selectByOwner(@Param("ownerCode") String ownerCode);

    @Select("SELECT * FROM wms_product WHERE sku_code = #{code}")
    Product selectByCode(@Param("code") String code);

    @Select(
            "SELECT * FROM wms_product WHERE category_code = #{categoryCode} AND status = 'ACTIVE' ORDER BY sku_code")
    List<Product> selectByCategory(@Param("categoryCode") String categoryCode);
}
