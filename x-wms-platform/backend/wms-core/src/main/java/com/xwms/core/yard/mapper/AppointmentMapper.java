package com.xwms.core.yard.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.yard.entity.Appointment;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    @Select(
            "SELECT * FROM wms_appointment WHERE warehouse_code = #{warehouse} AND plan_arrive_time BETWEEN #{start} AND #{end} AND status NOT IN ('CANCELLED','COMPLETED','NO_SHOW') AND deleted = 0 ORDER BY plan_arrive_time")
    List<Appointment> selectByDateRange(
            @Param("warehouse") String warehouse,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select(
            "SELECT COUNT(*) FROM wms_appointment WHERE dock_id = #{dockId} AND status IN ('CONFIRMED','ARRIVED','CHECKED_IN','LOADING') AND deleted = 0")
    int countActiveByDock(@Param("dockId") Long dockId);
}
