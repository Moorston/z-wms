package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.ReturnOrder;

/** 退货单Mapper */
@Mapper
public interface ReturnOrderMapper extends BaseMapper<ReturnOrder> {

    /** 根据退货单号查询 */
    ReturnOrder selectByReturnNo(@Param("returnNo") String returnNo);

    /** 根据原出库单号查询 */
    List<ReturnOrder> selectByOriginalOutboundNo(
            @Param("originalOutboundNo") String originalOutboundNo);

    /** 根据状态查询 */
    List<ReturnOrder> selectByStatus(@Param("status") String status);

    /** 更新状态 */
    int updateStatus(@Param("returnNo") String returnNo, @Param("status") String status);

    /** 更新收货数量 */
    int updateReceivedQty(
            @Param("returnNo") String returnNo,
            @Param("receivedQty") java.math.BigDecimal receivedQty);
}
