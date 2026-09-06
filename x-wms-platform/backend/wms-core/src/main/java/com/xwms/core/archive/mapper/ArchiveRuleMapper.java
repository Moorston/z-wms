package com.xwms.core.archive.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.archive.entity.ArchiveRule;

@Mapper
public interface ArchiveRuleMapper extends BaseMapper<ArchiveRule> {

    @Select("SELECT * FROM wms_archive_rule WHERE enabled = 1 ORDER BY rule_code")
    List<ArchiveRule> selectEnabled();

    @Update("UPDATE wms_archive_rule SET last_execute_time = NOW() WHERE id = #{id}")
    int updateLastExecuteTime(@Param("id") Long id);
}
