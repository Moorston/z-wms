package com.xwms.core.po.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.po.entity.PoAsnRelation;

/** PO-ASN关联Mapper */
@Mapper
public interface PoAsnRelationMapper extends BaseMapper<PoAsnRelation> {

    /** 根据关联号查询 */
    PoAsnRelation selectByRelationNo(@Param("relationNo") String relationNo);

    /** 根据PO号查询关联列表 */
    List<PoAsnRelation> selectByPoNo(@Param("poNo") String poNo);

    /** 根据ASN号查询关联列表 */
    List<PoAsnRelation> selectByAsnNo(@Param("asnNo") String asnNo);

    /** 根据PO号和明细号查询 */
    List<PoAsnRelation> selectByPoNoAndDetailNo(
            @Param("poNo") String poNo, @Param("poDetailNo") String poDetailNo);

    /** 更新关联收货数量 */
    int updateReceivedQty(
            @Param("relationNo") String relationNo,
            @Param("receivedQty") java.math.BigDecimal receivedQty);

    /** 更新关联入库数量 */
    int updatePutawayQty(
            @Param("relationNo") String relationNo,
            @Param("putawayQty") java.math.BigDecimal putawayQty);

    /** 更新关联状态 */
    int updateStatus(@Param("relationNo") String relationNo, @Param("status") String status);
}
