package com.xwms.core.qc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.qc.entity.QcSample;

/** 质检样本Mapper */
@Mapper
public interface QcSampleMapper extends BaseMapper<QcSample> {

    QcSample selectBySampleNo(@Param("sampleNo") String sampleNo);

    List<QcSample> selectByTaskNo(@Param("taskNo") String taskNo);

    List<QcSample> selectByQcNo(@Param("qcNo") String qcNo);

    int updateStatus(@Param("sampleNo") String sampleNo, @Param("status") String status);

    int updateTestResult(
            @Param("sampleNo") String sampleNo,
            @Param("testValue") String testValue,
            @Param("isQualified") String isQualified,
            @Param("defectType") String defectType,
            @Param("defectDesc") String defectDesc);
}
