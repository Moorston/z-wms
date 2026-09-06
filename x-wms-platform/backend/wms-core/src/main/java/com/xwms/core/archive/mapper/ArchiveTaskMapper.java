package com.xwms.core.archive.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.archive.entity.ArchiveTask;

@Mapper
public interface ArchiveTaskMapper extends BaseMapper<ArchiveTask> {

    @Select("SELECT * FROM wms_archive_task WHERE rule_id = #{ruleId} ORDER BY created_time DESC")
    List<ArchiveTask> selectByRuleId(@Param("ruleId") Long ruleId);
}
