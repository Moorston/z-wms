package com.xwms.core.shipment.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.shipment.entity.ShipmentTask;

@Mapper
public interface ShipmentTaskMapper extends BaseMapper<ShipmentTask> {

    @Select(
            "SELECT * FROM wms_shipment_task WHERE shipment_no = #{shipmentNo} ORDER BY created_time")
    List<ShipmentTask> selectByShipmentNo(@Param("shipmentNo") String shipmentNo);

    @Select("SELECT * FROM wms_shipment_task WHERE task_no = #{taskNo}")
    ShipmentTask selectByTaskNo(@Param("taskNo") String taskNo);
}
