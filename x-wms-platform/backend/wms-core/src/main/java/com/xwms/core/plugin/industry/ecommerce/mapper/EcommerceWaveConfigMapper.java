package com.xwms.core.plugin.industry.ecommerce.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.ecommerce.entity.EcommerceWaveConfig;

/** 电商波次配置Mapper */
@Mapper
public interface EcommerceWaveConfigMapper extends BaseMapper<EcommerceWaveConfig> {

    /** 根据配置编号查询 */
    EcommerceWaveConfig selectByConfigCode(@Param("configCode") String configCode);

    /** 查询启用的配置 */
    List<EcommerceWaveConfig> selectEnabledConfigs();

    /** 根据店铺和活动类型查询 */
    List<EcommerceWaveConfig> selectByShopAndActivityType(
            @Param("shopCode") String shopCode, @Param("activityType") String activityType);

    /** 根据活动类型查询启用的配置 */
    List<EcommerceWaveConfig> selectEnabledByActivityType(
            @Param("activityType") String activityType);
}
