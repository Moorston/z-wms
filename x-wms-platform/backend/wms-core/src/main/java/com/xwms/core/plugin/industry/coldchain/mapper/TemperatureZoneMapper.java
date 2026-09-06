package com.xwms.core.plugin.industry.coldchain.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.coldchain.entity.TemperatureZone;

/** 温区管理Mapper */
@Mapper
public interface TemperatureZoneMapper extends BaseMapper<TemperatureZone> {

    /** 根据温区编码查询 */
    TemperatureZone selectByZoneCode(@Param("zoneCode") String zoneCode);

    /** 根据仓库查询温区 */
    List<TemperatureZone> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    /** 根据温度类型查询 */
    List<TemperatureZone> selectByTemperatureType(@Param("temperatureType") String temperatureType);

    /** 根据库区编码查询所属温区 */
    TemperatureZone selectByAreaCode(@Param("areaCode") String areaCode);
}
