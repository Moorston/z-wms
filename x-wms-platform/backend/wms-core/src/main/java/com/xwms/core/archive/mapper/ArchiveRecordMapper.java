package com.xwms.core.archive.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.archive.entity.ArchiveRecord;

@Mapper
public interface ArchiveRecordMapper extends BaseMapper<ArchiveRecord> {

    @Select(
            "SELECT * FROM wms_archive_record WHERE source_table = #{tableName} AND source_id = #{sourceId}")
    ArchiveRecord selectBySource(
            @Param("tableName") String tableName, @Param("sourceId") Long sourceId);

    @Select("SELECT * FROM wms_archive_record WHERE batch_id = #{batchId} ORDER BY id")
    List<ArchiveRecord> selectByBatchId(@Param("batchId") Long batchId);
}
