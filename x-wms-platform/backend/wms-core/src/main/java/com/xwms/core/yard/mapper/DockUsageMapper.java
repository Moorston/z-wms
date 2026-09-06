package com.xwms.core.yard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.yard.entity.DockUsage;

@Mapper
public interface DockUsageMapper extends BaseMapper<DockUsage> {

    @Select("SELECT * FROM wms_dock_usage WHERE dock_id = #{dockId} ORDER BY occupy_start DESC")
    List<DockUsage> selectByDockId(@Param("dockId") Long dockId);

    @Select("SELECT * FROM wms_dock_usage WHERE status = 'ACTIVE' AND dock_id = #{dockId} LIMIT 1")
    DockUsage selectActiveByDock(@Param("dockId") Long dockId);
}
