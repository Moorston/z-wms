package com.xwms.core.plugin.industry.coldchain.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.coldchain.entity.ColdChainEquipment;

/** 冷链设备Mapper */
@Mapper
public interface ColdChainEquipmentMapper extends BaseMapper<ColdChainEquipment> {

    /** 根据设备编号查询 */
    ColdChainEquipment selectByEquipmentCode(@Param("equipmentCode") String equipmentCode);

    /** 根据仓库查询设备 */
    List<ColdChainEquipment> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    /** 根据设备类型查询 */
    List<ColdChainEquipment> selectByType(@Param("equipmentType") String equipmentType);

    /** 查询故障设备 */
    List<ColdChainEquipment> selectFaultEquipments();

    /** 查询需要维护的设备 */
    List<ColdChainEquipment> selectNeedMaintainEquipments();
}
