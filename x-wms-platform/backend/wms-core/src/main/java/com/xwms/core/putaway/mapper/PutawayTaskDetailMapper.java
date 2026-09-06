package com.xwms.core.putaway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putaway.entity.PutawayTaskDetail;

/** 上架任务明细Mapper */
@Mapper
public interface PutawayTaskDetailMapper extends BaseMapper<PutawayTaskDetail> {

    List<PutawayTaskDetail> selectByTaskNo(@Param("taskNo") String taskNo);

    PutawayTaskDetail selectByDetailNo(@Param("detailNo") String detailNo);

    PutawayTaskDetail selectByTaskNoAndSku(
            @Param("taskNo") String taskNo, @Param("skuCode") String skuCode);

    int updatePutawayQty(
            @Param("detailNo") String detailNo,
            @Param("putawayQty") java.math.BigDecimal putawayQty);

    int updateActualLocation(
            @Param("detailNo") String detailNo,
            @Param("actualLocation") String actualLocation,
            @Param("actualArea") String actualArea);

    int updateStatus(@Param("detailNo") String detailNo, @Param("status") String status);
}
