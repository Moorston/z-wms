package com.xwms.core.expiry.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.expiry.entity.ExpiryHandleLog;

@Mapper
public interface ExpiryHandleLogMapper extends BaseMapper<ExpiryHandleLog> {

    @Select("SELECT * FROM wms_expiry_handle_log WHERE handle_no = #{handleNo}")
    ExpiryHandleLog selectByHandleNo(@Param("handleNo") String handleNo);

    @Select(
            "SELECT * FROM wms_expiry_handle_log WHERE batch_no = #{batchNo} ORDER BY handle_time DESC")
    List<ExpiryHandleLog> selectByBatchNo(@Param("batchNo") String batchNo);
}
