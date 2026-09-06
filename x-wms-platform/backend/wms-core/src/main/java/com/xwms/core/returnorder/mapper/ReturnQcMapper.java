package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.ReturnQc;

@Mapper
public interface ReturnQcMapper extends BaseMapper<ReturnQc> {

    @Select("SELECT * FROM wms_return_qc WHERE return_id = #{returnId} ORDER BY id")
    List<ReturnQc> selectByReturnId(@Param("returnId") Long returnId);
}
