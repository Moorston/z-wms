package com.xwms.core.putaway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putaway.entity.PutawayRecord;

/** 上架记录Mapper */
@Mapper
public interface PutawayRecordMapper extends BaseMapper<PutawayRecord> {

    PutawayRecord selectByRecordNo(@Param("recordNo") String recordNo);

    List<PutawayRecord> selectByTaskNo(@Param("taskNo") String taskNo);

    List<PutawayRecord> selectByInboundNo(@Param("inboundNo") String inboundNo);

    List<PutawayRecord> selectBySkuCode(@Param("skuCode") String skuCode);

    List<PutawayRecord> selectByTargetLocation(@Param("targetLocation") String targetLocation);
}
