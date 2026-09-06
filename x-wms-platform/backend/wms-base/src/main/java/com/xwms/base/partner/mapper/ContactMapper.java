package com.xwms.base.partner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.partner.entity.Contact;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {

    @Select(
            "SELECT * FROM wms_contact WHERE partner_type = #{partnerType} AND partner_code = #{partnerCode} ORDER BY is_primary DESC, contact_code")
    List<Contact> selectByPartner(
            @Param("partnerType") String partnerType, @Param("partnerCode") String partnerCode);

    @Select("SELECT * FROM wms_contact WHERE contact_code = #{code}")
    Contact selectByCode(@Param("code") String code);
}
