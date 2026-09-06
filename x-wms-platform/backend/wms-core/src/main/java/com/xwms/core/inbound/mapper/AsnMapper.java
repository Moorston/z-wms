package com.xwms.core.inbound.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.inbound.entity.Asn;

@Mapper
public interface AsnMapper extends BaseMapper<Asn> {

    @Select("SELECT * FROM wms_asn WHERE asn_no = #{asnNo}")
    Asn selectByAsnNo(@Param("asnNo") String asnNo);

    @Select("SELECT * FROM wms_asn WHERE status = #{status} ORDER BY expected_date")
    List<Asn> selectByStatus(@Param("status") String status);
}
