package com.xwms.core.plugin.industry.coldchain.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.coldchain.entity.TemperatureRecord;

/** 温度监控记录Mapper */
@Mapper
public interface TemperatureRecordMapper extends BaseMapper<TemperatureRecord> {

    /** 根据记录编号查询 */
    TemperatureRecord selectByRecordNo(@Param("recordNo") String recordNo);

    /** 根据库位和时间范围查询 */
    List<TemperatureRecord> selectByLocationAndTime(
            @Param("locationCode") String locationCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /** 根据设备和时间范围查询 */
    List<TemperatureRecord> selectByEquipmentAndTime(
            @Param("equipmentCode") String equipmentCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /** 查询最新的温度记录 */
    TemperatureRecord selectLatestByLocation(@Param("locationCode") String locationCode);
}
