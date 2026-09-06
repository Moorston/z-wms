package com.xwms.core.transfer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transfer.entity.TransferInTransit;

@Mapper
public interface TransferInTransitMapper extends BaseMapper<TransferInTransit> {

    @Select("SELECT * FROM wms_transfer_in_transit WHERE transfer_no = #{transferNo} ORDER BY id")
    List<TransferInTransit> selectByTransferNo(@Param("transferNo") String transferNo);

    @Select(
            "SELECT * FROM wms_transfer_in_transit WHERE sku_code = #{skuCode} AND status != 'RECEIVED' ORDER BY ship_time")
    List<TransferInTransit> selectActiveBySku(@Param("skuCode") String skuCode);

    @Select(
            "SELECT * FROM wms_transfer_in_transit WHERE to_warehouse = #{warehouseCode} AND status != 'RECEIVED' ORDER BY expected_arrival")
    List<TransferInTransit> selectIncomingByWarehouse(@Param("warehouseCode") String warehouseCode);
}
