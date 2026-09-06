package com.xwms.core.putaway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putaway.entity.PutawayReasonCode;

/** 上架原因代码Mapper */
@Mapper
public interface PutawayReasonCodeMapper extends BaseMapper<PutawayReasonCode> {

    @Select(
            "SELECT * FROM wms_putaway_reason_code WHERE reason_type = #{reasonType} AND status = 'ACTIVE' ORDER BY sort_order")
    List<PutawayReasonCode> selectByType(@Param("reasonType") String reasonType);

    @Select("SELECT * FROM wms_putaway_reason_code WHERE reason_code = #{reasonCode}")
    PutawayReasonCode selectByCode(@Param("reasonCode") String reasonCode);
}
