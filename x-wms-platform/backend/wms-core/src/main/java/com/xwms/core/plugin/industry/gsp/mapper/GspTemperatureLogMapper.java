package com.xwms.core.plugin.industry.gsp.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.gsp.entity.GspTemperatureLog;

/** GSP温度日志Mapper */
@Mapper
public interface GspTemperatureLogMapper extends BaseMapper<GspTemperatureLog> {

    /** 根据日志编号查询 */
    GspTemperatureLog selectByLogNo(@Param("logNo") String logNo);

    /** 根据库区和时间范围查询 */
    List<GspTemperatureLog> selectByAreaAndTime(
            @Param("areaCode") String areaCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /** 查询超标记录 */
    List<GspTemperatureLog> selectExceededRecords();

    /** 查询待处理的超标记录 */
    List<GspTemperatureLog> selectPendingHandleRecords();

    /** 根据设备和时间范围查询 */
    List<GspTemperatureLog> selectByEquipmentAndTime(
            @Param("equipmentCode") String equipmentCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
