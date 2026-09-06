package com.xwms.core.transfer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transfer.entity.Transfer;

@Mapper
public interface TransferMapper extends BaseMapper<Transfer> {

    @Select("SELECT * FROM wms_transfer WHERE transfer_no = #{transferNo}")
    Transfer selectByTransferNo(@Param("transferNo") String transferNo);

    @Select(
            "SELECT * FROM wms_transfer WHERE from_warehouse = #{warehouseCode} ORDER BY created_time DESC")
    List<Transfer> selectByFromWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select(
            "SELECT * FROM wms_transfer WHERE to_warehouse = #{warehouseCode} ORDER BY created_time DESC")
    List<Transfer> selectByToWarehouse(@Param("warehouseCode") String warehouseCode);

    @Select("SELECT * FROM wms_transfer WHERE status = #{status} ORDER BY created_time")
    List<Transfer> selectByStatus(@Param("status") String status);
}
