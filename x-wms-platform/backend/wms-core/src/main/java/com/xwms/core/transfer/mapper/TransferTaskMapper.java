package com.xwms.core.transfer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transfer.entity.TransferTask;

@Mapper
public interface TransferTaskMapper extends BaseMapper<TransferTask> {

    @Select(
            "SELECT * FROM wms_transfer_task WHERE transfer_no = #{transferNo} ORDER BY created_time")
    List<TransferTask> selectByTransferNo(@Param("transferNo") String transferNo);

    @Select("SELECT * FROM wms_transfer_task WHERE task_no = #{taskNo}")
    TransferTask selectByTaskNo(@Param("taskNo") String taskNo);
}
