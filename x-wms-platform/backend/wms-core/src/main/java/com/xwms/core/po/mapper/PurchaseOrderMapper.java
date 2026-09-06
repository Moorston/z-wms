package com.xwms.core.po.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.po.entity.PurchaseOrder;

/** 采购订单Mapper */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /** 根据PO号查询 */
    PurchaseOrder selectByPoNo(@Param("poNo") String poNo);

    /** 根据外部订单号查询 */
    PurchaseOrder selectByExternalPoNo(@Param("externalPoNo") String externalPoNo);

    /** 查询待释放的PO列表 */
    List<PurchaseOrder> selectPendingRelease(@Param("warehouseCode") String warehouseCode);

    /** 查询待收货的PO列表 */
    List<PurchaseOrder> selectPendingReceive(@Param("warehouseCode") String warehouseCode);

    /** 更新PO收货数量 */
    int updateReceivedQty(
            @Param("poNo") String poNo, @Param("receivedQty") java.math.BigDecimal receivedQty);

    /** 更新PO状态 */
    int updateStatus(@Param("poNo") String poNo, @Param("status") String status);
}
