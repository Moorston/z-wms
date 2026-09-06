package com.xwms.core.simulation.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.simulation.entity.SchemeEvaluation;

@Mapper
public interface SchemeEvaluationMapper extends BaseMapper<SchemeEvaluation> {
    @Select("SELECT * FROM wms_scheme_evaluation WHERE evaluation_id = #{evaluationId}")
    SchemeEvaluation selectByEvaluationId(@Param("evaluationId") String evaluationId);

    @Select(
            "SELECT * FROM wms_scheme_evaluation WHERE warehouse_code = #{warehouseCode} AND scheme_type = #{schemeType} ORDER BY created_time DESC")
    List<SchemeEvaluation> selectByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode, @Param("schemeType") String schemeType);

    @Select(
            "SELECT * FROM wms_scheme_evaluation WHERE status = #{status} ORDER BY created_time DESC")
    List<SchemeEvaluation> selectByStatus(@Param("status") String status);
}
