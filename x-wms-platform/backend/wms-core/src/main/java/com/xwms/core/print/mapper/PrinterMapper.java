package com.xwms.core.print.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.print.entity.Printer;

@Mapper
public interface PrinterMapper extends BaseMapper<Printer> {

    @Select(
            "SELECT * FROM wms_printer WHERE warehouse_code = #{whCode} AND enabled = 1 ORDER BY printer_code")
    List<Printer> selectByWarehouse(@Param("whCode") String whCode);

    @Select("SELECT * FROM wms_printer WHERE status = 'ONLINE' AND enabled = 1")
    List<Printer> selectOnlinePrinters();
}
