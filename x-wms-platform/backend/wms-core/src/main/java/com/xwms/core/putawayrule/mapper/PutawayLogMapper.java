package com.xwms.core.putawayrule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putawayrule.entity.PutawayLog;

/** 上架执行日志Mapper */
@Mapper
public interface PutawayLogMapper extends BaseMapper<PutawayLog> {

    /** 根据日志编号查询 */
    PutawayLog selectByLogNo(@Param("logNo") String logNo);

    /** 根据入库单号查询 */
    List<PutawayLog> selectByInboundNo(@Param("inboundNo") String inboundNo);

    /** 根据商品编码查询 */
    List<PutawayLog> selectBySkuCode(@Param("skuCode") String skuCode);
}
