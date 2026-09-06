package com.xwms.core.pack.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pack.entity.CheckRecord;

@Mapper
public interface CheckRecordMapper extends BaseMapper<CheckRecord> {

    @Select("SELECT * FROM wms_check_record WHERE pack_no = #{packNo} ORDER BY check_time")
    List<CheckRecord> selectByPackNo(@Param("packNo") String packNo);

    @Select("SELECT * FROM wms_check_record WHERE outbound_no = #{outboundNo} ORDER BY check_time")
    List<CheckRecord> selectByOutboundNo(@Param("outboundNo") String outboundNo);
}
