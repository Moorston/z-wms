package com.xwms.core.archive.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.archive.entity.ArchiveBatch;

@Mapper
public interface ArchiveBatchMapper extends BaseMapper<ArchiveBatch> {

    @Select("SELECT * FROM wms_archive_batch WHERE task_id = #{taskId} ORDER BY batch_index")
    List<ArchiveBatch> selectByTaskId(@Param("taskId") Long taskId);
}
