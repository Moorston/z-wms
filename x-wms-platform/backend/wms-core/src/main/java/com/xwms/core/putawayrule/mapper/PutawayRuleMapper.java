package com.xwms.core.putawayrule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putawayrule.entity.PutawayRule;

/** 上架规则Mapper */
@Mapper
public interface PutawayRuleMapper extends BaseMapper<PutawayRule> {

    /** 根据规则编码查询 */
    PutawayRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    /** 根据商品和货主查询匹配的规则(按优先级排序) */
    List<PutawayRule> selectMatchedRules(
            @Param("skuCode") String skuCode,
            @Param("categoryCode") String categoryCode,
            @Param("ownerCode") String ownerCode,
            @Param("warehouseCode") String warehouseCode);

    /** 根据商品和货主查询匹配的规则ID列表(用于缓存) 匹配逻辑：规则的skuCode/ownerCode/warehouseCode为空或等于传入值，状态为ACTIVE */
    @Select(
            "SELECT id FROM wms_putaway_rule WHERE status = 'ACTIVE' "
                    + "AND (sku_code IS NULL OR sku_code = '' OR sku_code = #{skuCode}) "
                    + "AND (owner_code IS NULL OR owner_code = '' OR owner_code = #{ownerCode}) "
                    + "AND (warehouse_code IS NULL OR warehouse_code = '' OR warehouse_code = #{warehouseCode}) "
                    + "ORDER BY priority ASC")
    List<Long> selectMatchedRuleIds(
            @Param("skuCode") String skuCode,
            @Param("ownerCode") String ownerCode,
            @Param("warehouseCode") String warehouseCode);

    /** 查询所有启用的规则 */
    List<PutawayRule> selectAllEnabled();
}
