package com.xwms.core.visualreceipt.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.visualreceipt.entity.ProductImage;

@Mapper
public interface ProductImageMapper extends BaseMapper<ProductImage> {
    List<ProductImage> selectBySku(@Param("skuCode") String skuCode);

    ProductImage selectByCode(@Param("imageCode") String imageCode);
}
