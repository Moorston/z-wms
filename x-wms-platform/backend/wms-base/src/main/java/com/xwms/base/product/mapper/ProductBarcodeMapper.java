package com.xwms.base.product.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.product.entity.ProductBarcode;

@Mapper
public interface ProductBarcodeMapper extends BaseMapper<ProductBarcode> {

    @Select(
            "SELECT * FROM wms_product_barcode WHERE sku_code = #{skuCode} AND status = 'ACTIVE' ORDER BY is_primary DESC, barcode")
    List<ProductBarcode> selectBySku(@Param("skuCode") String skuCode);

    @Select("SELECT * FROM wms_product_barcode WHERE barcode = #{barcode}")
    ProductBarcode selectByBarcode(@Param("barcode") String barcode);
}
