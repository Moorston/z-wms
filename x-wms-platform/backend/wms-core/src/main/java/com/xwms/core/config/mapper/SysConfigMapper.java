package com.xwms.core.config.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.config.entity.SysConfig;

/** 系统参数Mapper */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /** 根据参数编码查询（全局参数） */
    SysConfig selectByCode(@Param("configCode") String configCode);

    /** 根据参数编码和仓库查询（仓库级参数覆盖） */
    SysConfig selectByCodeAndWarehouse(
            @Param("configCode") String configCode, @Param("warehouseCode") String warehouseCode);

    /** 根据参数编码和货主查询（货主级参数覆盖） */
    SysConfig selectByCodeAndOwner(
            @Param("configCode") String configCode, @Param("ownerCode") String ownerCode);

    /** 根据分类查询参数列表 */
    List<SysConfig> selectByCategory(@Param("category") String category);

    /** 根据模块查询参数列表 */
    List<SysConfig> selectByModule(@Param("moduleCode") String moduleCode);

    /** 查询所有启用的参数 */
    List<SysConfig> selectAllEnabled();

    /** 更新参数值 */
    int updateValue(
            @Param("configCode") String configCode,
            @Param("configValue") String configValue,
            @Param("updatedBy") String updatedBy);
}
