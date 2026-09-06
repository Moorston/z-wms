package com.xwms.core.crossdock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.crossdock.entity.CrossdockDetail;

@Mapper
public interface CrossdockDetailMapper extends BaseMapper<CrossdockDetail> {

    @Select(
            "SELECT * FROM wms_crossdock_detail WHERE crossdock_no = #{crossdockNo} ORDER BY line_no")
    List<CrossdockDetail> selectByCrossdockNo(@Param("crossdockNo") String crossdockNo);
}
