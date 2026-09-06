package com.xwms.core.plugin.industry.ecommerce.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.ecommerce.entity.PreSaleOrder;

/** 预售订单Mapper */
@Mapper
public interface PreSaleOrderMapper extends BaseMapper<PreSaleOrder> {

    /** 根据预售单号查询 */
    PreSaleOrder selectByPreSaleNo(@Param("preSaleNo") String preSaleNo);

    /** 根据订单号查询 */
    PreSaleOrder selectByOrderNo(@Param("orderNo") String orderNo);

    /** 查询待付尾款的预售单 */
    List<PreSaleOrder> selectPendingBalanceOrders();

    /** 查询待发货的预售单 */
    List<PreSaleOrder> selectPendingShipOrders();

    /** 根据店铺和状态查询 */
    List<PreSaleOrder> selectByShopAndStatus(
            @Param("shopCode") String shopCode, @Param("status") String status);
}
