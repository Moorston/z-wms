package com.xwms.core.snapshot.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.snapshot.entity.SnapshotCompareDetail;

@Mapper
public interface SnapshotCompareDetailMapper extends BaseMapper<SnapshotCompareDetail> {

    @Select(
            "SELECT * FROM wms_snapshot_compare_detail WHERE compare_no = #{compareNo} ORDER BY diff_qty DESC")
    List<SnapshotCompareDetail> selectByCompareNo(@Param("compareNo") String compareNo);
}
