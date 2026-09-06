package com.xwms.core.qc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xwms.core.qc.entity.QcOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QcOrderMapper extends BaseMapper<QcOrder> {

    /**
     * 根据关联单据查询质检单
     */
    @Select("SELECT * FROM wms_qc_order WHERE ref_type = #{refType} AND ref_no = #{refNo} AND deleted = 0")
    List<QcOrder> selectByRef(@Param("refType") String refType, @Param("refNo") String refNo);

    /**
     * 查询待检质检单
     */
    @Select("SELECT * FROM wms_qc_order WHERE status = 'PENDING' AND warehouse_code_col = #{warehouseCode} AND deleted = 0 ORDER BY created_time")
    List<QcOrder> selectPending(@Param("warehouseCode") String warehouseCode);
}
