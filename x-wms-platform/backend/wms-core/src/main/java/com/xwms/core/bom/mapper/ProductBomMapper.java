package com.xwms.core.bom.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.bom.entity.ProductBom;

/** 产品BOM Mapper */
@Mapper
public interface ProductBomMapper extends BaseMapper<ProductBom> {

    /** 根据BOM编码查询 */
    ProductBom selectByBomCode(@Param("bomCode") String bomCode);

    /** 根据父件商品编码查询默认BOM */
    ProductBom selectDefaultByParentSku(@Param("parentSkuCode") String parentSkuCode);

    /** 查询所有启用的BOM */
    List<ProductBom> selectAllEnabled();
}
