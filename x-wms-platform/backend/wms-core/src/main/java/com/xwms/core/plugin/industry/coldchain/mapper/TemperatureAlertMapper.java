package com.xwms.core.plugin.industry.coldchain.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.coldchain.entity.TemperatureAlert;

/** 温度异常报警Mapper */
@Mapper
public interface TemperatureAlertMapper extends BaseMapper<TemperatureAlert> {

    /** 根据报警编号查询 */
    TemperatureAlert selectByAlertNo(@Param("alertNo") String alertNo);

    /** 查询待处理的报警 */
    List<TemperatureAlert> selectPendingAlerts();

    /** 根据库位查询报警 */
    List<TemperatureAlert> selectByLocation(@Param("locationCode") String locationCode);

    /** 根据设备查询报警 */
    List<TemperatureAlert> selectByEquipment(@Param("equipmentCode") String equipmentCode);

    /** 统计待处理报警数量 */
    int countPendingAlerts();
}
