package com.xwms.core.replenish.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.replenish.entity.ReplenishRule;

@Mapper
public interface ReplenishRuleMapper extends BaseMapper<ReplenishRule> {

    /** 匹配补货规则: SKU+拣货区 > SKU > 通用 */
    @Select(
            "SELECT * FROM wms_replenish_rule WHERE sku = #{sku} AND pick_area_code = #{pickArea} AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    ReplenishRule matchRule(@Param("sku") String sku, @Param("pickArea") String pickArea);

    @Select(
            "SELECT * FROM wms_replenish_rule WHERE sku = #{sku} AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    ReplenishRule matchSkuRule(@Param("sku") String sku);
}
