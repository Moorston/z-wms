package com.xwms.base.partner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.partner.entity.Supplier;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    @Select(
            "SELECT * FROM wms_supplier WHERE owner_code_col = #{ownerCode} AND status = 'ACTIVE' ORDER BY supplier_code")
    List<Supplier> selectByOwner(@Param("ownerCode") String ownerCode);

    @Select("SELECT * FROM wms_supplier WHERE supplier_code = #{code}")
    Supplier selectByCode(@Param("code") String code);
}
