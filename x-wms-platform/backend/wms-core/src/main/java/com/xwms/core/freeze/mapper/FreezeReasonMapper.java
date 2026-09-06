package com.xwms.core.freeze.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.freeze.entity.FreezeReason;

@Mapper
public interface FreezeReasonMapper extends BaseMapper<FreezeReason> {

    @Select("SELECT * FROM wms_freeze_reason WHERE reason_code = #{reasonCode}")
    FreezeReason selectByReasonCode(@Param("reasonCode") String reasonCode);
}
