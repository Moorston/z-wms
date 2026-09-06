package com.xwms.core.print.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.print.entity.PrintQueue;

@Mapper
public interface PrintQueueMapper extends BaseMapper<PrintQueue> {

    @Select(
            "SELECT * FROM wms_print_queue WHERE printer_code = #{printerCode} AND status = 'WAITING' ORDER BY priority ASC, created_time ASC")
    List<PrintQueue> selectWaitingByPrinter(@Param("printerCode") String printerCode);
}
