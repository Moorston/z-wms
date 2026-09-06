package com.xwms.core.crossdock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.crossdock.entity.CrossdockMatch;

@Mapper
public interface CrossdockMatchMapper extends BaseMapper<CrossdockMatch> {

    @Select("SELECT * FROM wms_crossdock_match WHERE crossdock_no = #{crossdockNo} ORDER BY id")
    List<CrossdockMatch> selectByCrossdockNo(@Param("crossdockNo") String crossdockNo);

    @Select("SELECT * FROM wms_crossdock_match WHERE match_no = #{matchNo}")
    CrossdockMatch selectByMatchNo(@Param("matchNo") String matchNo);
}
