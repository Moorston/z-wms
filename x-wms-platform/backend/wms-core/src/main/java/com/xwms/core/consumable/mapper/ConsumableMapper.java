package com.xwms.core.consumable.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.consumable.entity.Consumable;

/** 耗材 Mapper */
@Mapper
public interface ConsumableMapper extends BaseMapper<Consumable> {

    /** 根据耗材编码查询 */
    Consumable selectByCode(@Param("consumableCode") String consumableCode);

    /** 查询所有启用的耗材 */
    List<Consumable> selectAllEnabled();

    /** 更新库存 */
    int updateStock(
            @Param("consumableCode") String consumableCode, @Param("qty") java.math.BigDecimal qty);
}
