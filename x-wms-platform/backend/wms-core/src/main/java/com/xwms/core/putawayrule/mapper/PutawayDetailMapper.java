package com.xwms.core.putawayrule.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.putawayrule.entity.PutawayDetail;

/** 上架执行详情Mapper */
@Mapper
public interface PutawayDetailMapper extends BaseMapper<PutawayDetail> {

    /** 根据详情编号查询 */
    PutawayDetail selectByDetailNo(@Param("detailNo") String detailNo);

    /** 根据日志编号查询 */
    List<PutawayDetail> selectByLogNo(@Param("logNo") String logNo);

    /** 根据入库单号查询 */
    List<PutawayDetail> selectByInboundNo(@Param("inboundNo") String inboundNo);
}
