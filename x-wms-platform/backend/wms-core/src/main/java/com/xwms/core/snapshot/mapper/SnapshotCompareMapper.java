package com.xwms.core.snapshot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.snapshot.entity.SnapshotCompare;

@Mapper
public interface SnapshotCompareMapper extends BaseMapper<SnapshotCompare> {

    @Select("SELECT * FROM wms_snapshot_compare WHERE compare_no = #{compareNo}")
    SnapshotCompare selectByCompareNo(@Param("compareNo") String compareNo);
}
