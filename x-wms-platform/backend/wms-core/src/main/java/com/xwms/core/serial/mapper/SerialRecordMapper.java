package com.xwms.core.serial.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.serial.entity.SerialRecord;

/** 序列号采集记录Mapper */
@Mapper
public interface SerialRecordMapper extends BaseMapper<SerialRecord> {

    /** 根据采集单号查询 */
    SerialRecord selectByRecordNo(@Param("recordNo") String recordNo);

    /** 根据关联单据查询 */
    List<SerialRecord> selectByRefNo(@Param("refNo") String refNo);

    /** 根据业务类型和关联单据查询 */
    SerialRecord selectByBusinessAndRef(
            @Param("businessType") String businessType, @Param("refNo") String refNo);
}
