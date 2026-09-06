package com.xwms.core.qc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.qc.entity.QcSamplingPlan;

@Mapper
public interface QcSamplingPlanMapper extends BaseMapper<QcSamplingPlan> {

    /** 根据批量和检验水平查询抽样方案 */
    @Select(
            "SELECT * FROM wms_qc_sampling_plan WHERE inspection_level = #{level} AND lot_size_from <= #{lotSize} AND lot_size_to >= #{lotSize} AND strictness = #{strictness} LIMIT 1")
    QcSamplingPlan selectByLotSize(
            @Param("level") String level,
            @Param("lotSize") Integer lotSize,
            @Param("strictness") String strictness);
}
