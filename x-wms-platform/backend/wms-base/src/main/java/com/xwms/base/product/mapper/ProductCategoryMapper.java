package com.xwms.base.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.product.entity.ProductCategory;

@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

    @Select(
            "SELECT * FROM wms_product_category WHERE parent_code = #{parentCode} AND status = 'ACTIVE' ORDER BY sort_order")
    List<ProductCategory> selectByParent(@Param("parentCode") String parentCode);

    @Select("SELECT * FROM wms_product_category WHERE category_code = #{code}")
    ProductCategory selectByCode(@Param("code") String code);
}
