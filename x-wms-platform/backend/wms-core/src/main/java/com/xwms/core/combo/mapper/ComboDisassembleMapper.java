package com.xwms.core.combo.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.combo.entity.ComboDisassemble;

@Mapper
public interface ComboDisassembleMapper extends BaseMapper<ComboDisassemble> {

    @Select("SELECT * FROM wms_combo_disassemble WHERE disassemble_no = #{disassembleNo}")
    ComboDisassemble selectByDisassembleNo(@Param("disassembleNo") String disassembleNo);

    @Update(
            "UPDATE wms_combo_disassemble SET status = #{status}, disassemble_time = NOW(), updated_time = NOW() WHERE disassemble_no = #{disassembleNo}")
    int updateStatus(@Param("disassembleNo") String disassembleNo, @Param("status") String status);
}
