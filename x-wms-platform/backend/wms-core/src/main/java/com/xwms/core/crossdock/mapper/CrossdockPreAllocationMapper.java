package com.xwms.core.crossdock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.crossdock.entity.CrossdockPreAllocation;

/** 越库预配Mapper */
@Mapper
public interface CrossdockPreAllocationMapper extends BaseMapper<CrossdockPreAllocation> {

    /** 根据预配单号查询 */
    CrossdockPreAllocation selectByAllocNo(@Param("allocNo") String allocNo);

    /** 根据越库单号查询 */
    List<CrossdockPreAllocation> selectByCrossdockNo(@Param("crossdockNo") String crossdockNo);

    /** 根据ASN号查询 */
    List<CrossdockPreAllocation> selectByAsnNo(@Param("asnNo") String asnNo);

    /** 根据出库单号查询 */
    List<CrossdockPreAllocation> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    /** 批量插入 */
    int batchInsert(@Param("list") List<CrossdockPreAllocation> list);
}
