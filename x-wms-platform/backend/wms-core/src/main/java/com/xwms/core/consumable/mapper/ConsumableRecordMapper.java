package com.xwms.core.consumable.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.consumable.entity.ConsumableRecord;

/** 耗材扣减记录 Mapper */
@Mapper
public interface ConsumableRecordMapper extends BaseMapper<ConsumableRecord> {

    /** 根据记录单号查询 */
    ConsumableRecord selectByRecordNo(@Param("recordNo") String recordNo);

    /** 根据关联单据查询 */
    List<ConsumableRecord> selectByRefNo(@Param("refNo") String refNo);

    /** 批量插入 */
    int batchInsert(@Param("list") List<ConsumableRecord> list);
}
