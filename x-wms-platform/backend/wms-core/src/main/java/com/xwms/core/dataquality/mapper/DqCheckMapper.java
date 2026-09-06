package com.xwms.core.dataquality.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.dataquality.entity.DqCheck;

@Mapper
public interface DqCheckMapper extends BaseMapper<DqCheck> {
    @Select("SELECT * FROM wms_dq_check WHERE check_id = #{checkId}")
    DqCheck selectByCheckId(@Param("checkId") String checkId);

    @Select(
            "SELECT * FROM wms_dq_check WHERE rule_id = #{ruleId} ORDER BY check_start_time DESC LIMIT #{limit}")
    List<DqCheck> selectRecentByRule(@Param("ruleId") String ruleId, @Param("limit") int limit);

    @Select(
            "SELECT * FROM wms_dq_check WHERE warehouse_code = #{warehouseCode} AND check_start_time >= #{startTime} AND check_start_time <= #{endTime} ORDER BY check_start_time")
    List<DqCheck> selectByWarehouseAndTime(
            @Param("warehouseCode") String warehouseCode,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM wms_dq_check WHERE status = #{status} ORDER BY created_time DESC")
    List<DqCheck> selectByStatus(@Param("status") String status);
}
