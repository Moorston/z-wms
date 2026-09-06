package com.xwms.core.po.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.po.entity.PurchaseOrderDetail;

/** 采购订单明细Mapper */
@Mapper
public interface PurchaseOrderDetailMapper extends BaseMapper<PurchaseOrderDetail> {

    /** 根据PO号查询明细列表 */
    List<PurchaseOrderDetail> selectByPoNo(@Param("poNo") String poNo);

    /** 根据明细号查询 */
    PurchaseOrderDetail selectByDetailNo(@Param("detailNo") String detailNo);

    /** 根据PO号和行号查询 */
    PurchaseOrderDetail selectByPoNoAndLineNo(
            @Param("poNo") String poNo, @Param("lineNo") Integer lineNo);

    /** 查询可释放的明细（orderQty > releasedQty） */
    List<PurchaseOrderDetail> selectReleasableDetails(@Param("poNo") String poNo);

    /** 更新明细释放数量 */
    int updateReleasedQty(
            @Param("detailNo") String detailNo,
            @Param("releasedQty") java.math.BigDecimal releasedQty);

    /** 更新明细收货数量 */
    int updateReceivedQty(
            @Param("detailNo") String detailNo,
            @Param("receivedQty") java.math.BigDecimal receivedQty);
}
