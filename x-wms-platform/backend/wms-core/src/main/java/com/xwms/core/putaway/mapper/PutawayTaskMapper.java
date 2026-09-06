package com.xwms.core.putaway.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putaway.entity.PutawayTask;

/** 上架任务Mapper */
@Mapper
public interface PutawayTaskMapper extends BaseMapper<PutawayTask> {

    PutawayTask selectByTaskNo(@Param("taskNo") String taskNo);

    PutawayTask selectByInboundNo(@Param("inboundNo") String inboundNo);

    List<PutawayTask> selectByAsnNo(@Param("asnNo") String asnNo);

    List<PutawayTask> selectPendingTasks(@Param("warehouseCode") String warehouseCode);

    int updatePutawayQty(
            @Param("taskNo") String taskNo, @Param("putawayQty") java.math.BigDecimal putawayQty);

    int updateStatus(@Param("taskNo") String taskNo, @Param("status") String status);
}
