package com.xwms.core.bom.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.bom.entity.ProductBomDetail;

/** 产品BOM明细 Mapper */
@Mapper
public interface ProductBomDetailMapper extends BaseMapper<ProductBomDetail> {

    /** 根据BOM编码查询明细 */
    List<ProductBomDetail> selectByBomCode(@Param("bomCode") String bomCode);

    /** 批量插入 */
    int batchInsert(@Param("list") List<ProductBomDetail> list);
}
