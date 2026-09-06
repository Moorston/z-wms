package com.xwms.core.pack.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pack.entity.PackDetail;

@Mapper
public interface PackDetailMapper extends BaseMapper<PackDetail> {

    @Select("SELECT * FROM wms_pack_detail WHERE pack_no = #{packNo} ORDER BY line_no")
    List<PackDetail> selectByPackNo(@Param("packNo") String packNo);
}
