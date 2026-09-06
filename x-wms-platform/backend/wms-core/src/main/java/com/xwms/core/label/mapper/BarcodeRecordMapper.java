package com.xwms.core.label.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.label.entity.BarcodeRecord;

@Mapper
public interface BarcodeRecordMapper extends BaseMapper<BarcodeRecord> {

    @Select("SELECT * FROM wms_barcode_record WHERE record_id = #{recordId}")
    BarcodeRecord selectByRecordId(@Param("recordId") String recordId);

    @Select("SELECT * FROM wms_barcode_record WHERE barcode = #{barcode}")
    BarcodeRecord selectByBarcode(@Param("barcode") String barcode);

    @Select(
            "SELECT * FROM wms_barcode_record WHERE barcode_type = #{barcodeType} AND biz_key = #{bizKey} AND status = 'ACTIVE'")
    List<BarcodeRecord> selectByTypeAndBizKey(
            @Param("barcodeType") String barcodeType, @Param("bizKey") String bizKey);

    @Select(
            "SELECT * FROM wms_barcode_record WHERE warehouse_code = #{warehouseCode} AND barcode_type = #{barcodeType} ORDER BY created_time DESC LIMIT #{limit}")
    List<BarcodeRecord> selectRecentByWarehouseAndType(
            @Param("warehouseCode") String warehouseCode,
            @Param("barcodeType") String barcodeType,
            @Param("limit") int limit);

    @Update(
            "UPDATE wms_barcode_record SET print_count = print_count + 1, last_print_time = NOW(), updated_time = NOW() WHERE record_id = #{recordId}")
    int incrementPrintCount(@Param("recordId") String recordId);

    @Update(
            "UPDATE wms_barcode_record SET scan_count = scan_count + 1, last_scan_time = NOW(), updated_time = NOW() WHERE record_id = #{recordId}")
    int incrementScanCount(@Param("recordId") String recordId);

    @Update(
            "UPDATE wms_barcode_record SET status = #{status}, updated_time = NOW() WHERE record_id = #{recordId}")
    int updateStatus(@Param("recordId") String recordId, @Param("status") String status);
}
