package com.xwms.core.adjust.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.adjust.entity.InventoryAdjustDetail;

@Mapper
public interface InventoryAdjustDetailMapper extends BaseMapper<InventoryAdjustDetail> {

    @Select(
            "SELECT * FROM wms_inventory_adjust_detail WHERE adjust_no = #{adjustNo} ORDER BY line_no")
    List<InventoryAdjustDetail> selectByAdjustNo(@Param("adjustNo") String adjustNo);
}
