package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.SowingTask;

/** 播种任务Mapper */
@Mapper
public interface SowingTaskMapper extends BaseMapper<SowingTask> {

    /** 根据任务号查询 */
    SowingTask selectByTaskNo(@Param("taskNo") String taskNo);

    /** 根据编组号查询 */
    List<SowingTask> selectByGroupNo(@Param("groupNo") String groupNo);

    /** 根据编组号和阶段查询 */
    List<SowingTask> selectByGroupNoAndStage(
            @Param("groupNo") String groupNo, @Param("sowingStage") String sowingStage);

    /** 根据状态查询 */
    List<SowingTask> selectByStatus(@Param("status") String status);

    /** 更新状态 */
    int updateStatus(@Param("taskNo") String taskNo, @Param("status") String status);

    /** 批量插入 */
    int batchInsert(@Param("list") List<SowingTask> list);
}
