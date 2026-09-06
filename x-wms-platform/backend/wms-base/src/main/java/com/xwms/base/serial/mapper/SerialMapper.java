package com.xwms.base.serial.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.base.serial.entity.Serial;

@Mapper
public interface SerialMapper extends BaseMapper<Serial> {

    @Select("SELECT * FROM wms_serial WHERE serial_no = #{serialNo}")
    Serial selectBySerialNo(@Param("serialNo") String serialNo);

    @Select(
            "SELECT * FROM wms_serial WHERE sku_code = #{skuCode} AND status = #{status} ORDER BY serial_no")
    List<Serial> selectBySkuAndStatus(
            @Param("skuCode") String skuCode, @Param("status") String status);

    @Select("SELECT * FROM wms_serial WHERE batch_no = #{batchNo} ORDER BY serial_no")
    List<Serial> selectByBatchNo(@Param("batchNo") String batchNo);

    @Select("SELECT * FROM wms_serial WHERE location_code = #{locationCode} ORDER BY serial_no")
    List<Serial> selectByLocation(@Param("locationCode") String locationCode);

    @Select("SELECT * FROM wms_serial WHERE container_no = #{containerNo} ORDER BY serial_no")
    List<Serial> selectByContainer(@Param("containerNo") String containerNo);

    @Select("SELECT * FROM wms_serial WHERE outbound_no = #{outboundNo} ORDER BY serial_no")
    List<Serial> selectByOutbound(@Param("outboundNo") String outboundNo);

    @Select("SELECT COUNT(*) FROM wms_serial WHERE sku_code = #{skuCode} AND status = 'IN_STOCK'")
    int countInStockBySku(@Param("skuCode") String skuCode);

    /** 更新序列号状态 */
    @Update(
            "UPDATE wms_serial SET status = #{status}, updated_time = NOW() WHERE serial_no = #{serialNo}")
    int updateStatus(@Param("serialNo") String serialNo, @Param("status") String status);

    /** 更新序列号位置 */
    @Update(
            "UPDATE wms_serial SET location_code = #{locationCode}, container_no = #{containerNo}, updated_time = NOW() WHERE serial_no = #{serialNo}")
    int updateLocation(
            @Param("serialNo") String serialNo,
            @Param("locationCode") String locationCode,
            @Param("containerNo") String containerNo);
}
