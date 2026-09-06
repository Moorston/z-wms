package com.xwms.core.consumable.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.consumable.entity.ProductPackaging;

/** 产品包装关联 Mapper */
@Mapper
public interface ProductPackagingMapper extends BaseMapper<ProductPackaging> {

    /** 根据包装代码查询 */
    ProductPackaging selectByCode(@Param("packagingCode") String packagingCode);

    /** 根据商品编码查询包装关联 */
    List<ProductPackaging> selectBySku(@Param("skuCode") String skuCode);

    /** 根据商品编码和包装层级查询 */
    List<ProductPackaging> selectBySkuAndLevel(
            @Param("skuCode") String skuCode, @Param("packagingLevel") Integer packagingLevel);
}
