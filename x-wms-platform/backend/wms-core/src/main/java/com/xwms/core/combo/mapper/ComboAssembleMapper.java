package com.xwms.core.combo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.combo.entity.ComboAssemble;

@Mapper
public interface ComboAssembleMapper extends BaseMapper<ComboAssemble> {

    @Select("SELECT * FROM wms_combo_assemble WHERE assemble_no = #{assembleNo}")
    ComboAssemble selectByAssembleNo(@Param("assembleNo") String assembleNo);

    @Update(
            "UPDATE wms_combo_assemble SET status = #{status}, assemble_time = NOW(), updated_time = NOW() WHERE assemble_no = #{assembleNo}")
    int updateStatus(@Param("assembleNo") String assembleNo, @Param("status") String status);
}
