package com.xwms.base.partner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.partner.entity.Owner;

@Mapper
public interface OwnerMapper extends BaseMapper<Owner> {

    @Select("SELECT * FROM wms_owner WHERE status = 'ACTIVE' ORDER BY owner_code")
    List<Owner> selectActiveOwners();

    @Select("SELECT * FROM wms_owner WHERE owner_code = #{code}")
    Owner selectByCode(@Param("code") String code);
}
