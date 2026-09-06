package com.xwms.core.pool.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pool.entity.ShareRule;

@Mapper
public interface ShareRuleMapper extends BaseMapper<ShareRule> {

    @Select("SELECT * FROM wms_share_rule WHERE rule_code = #{ruleCode}")
    ShareRule selectByRuleCode(@Param("ruleCode") String ruleCode);

    @Select("SELECT * FROM wms_share_rule WHERE pool_code = #{poolCode} AND status = 'ACTIVE'")
    List<ShareRule> selectByPoolCode(@Param("poolCode") String poolCode);

    @Select(
            "SELECT * FROM wms_share_rule WHERE warehouse_code = #{warehouseCode} AND share_type = #{shareType} AND status = 'ACTIVE' ORDER BY priority DESC")
    List<ShareRule> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("shareType") String shareType);

    @Select(
            "SELECT * FROM wms_share_rule WHERE source_owner = #{sourceOwner} AND target_owner = #{targetOwner} AND status = 'ACTIVE'")
    List<ShareRule> selectByOwnerPair(
            @Param("sourceOwner") String sourceOwner, @Param("targetOwner") String targetOwner);
}
