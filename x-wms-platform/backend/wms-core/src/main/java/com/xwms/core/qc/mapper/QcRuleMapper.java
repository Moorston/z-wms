package com.xwms.core.qc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.qc.entity.QcRule;

@Mapper
public interface QcRuleMapper extends BaseMapper<QcRule> {

    /** 匹配质检规则: 优先SKU+供应商，其次通用规则 */
    @Select(
            "SELECT * FROM wms_qc_rule WHERE sku = #{sku} AND supplier_code = #{supplierCode} AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    QcRule matchRule(@Param("sku") String sku, @Param("supplierCode") String supplierCode);

    @Select(
            "SELECT * FROM wms_qc_rule WHERE sku = #{sku} AND supplier_code IS NULL AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    QcRule matchSkuRule(@Param("sku") String sku);

    @Select(
            "SELECT * FROM wms_qc_rule WHERE sku IS NULL AND supplier_code IS NULL AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    QcRule matchDefaultRule();
}
