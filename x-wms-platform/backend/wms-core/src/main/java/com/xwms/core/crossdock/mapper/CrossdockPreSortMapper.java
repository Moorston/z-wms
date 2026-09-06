package com.xwms.core.crossdock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.crossdock.entity.CrossdockPreSort;

/** 越库预分Mapper */
@Mapper
public interface CrossdockPreSortMapper extends BaseMapper<CrossdockPreSort> {

    /** 根据预分单号查询 */
    CrossdockPreSort selectBySortNo(@Param("sortNo") String sortNo);

    /** 根据越库单号查询 */
    List<CrossdockPreSort> selectByCrossdockNo(@Param("crossdockNo") String crossdockNo);

    /** 根据箱号查询 */
    CrossdockPreSort selectByBoxNo(@Param("boxNo") String boxNo);

    /** 根据ASN号查询 */
    List<CrossdockPreSort> selectByAsnNo(@Param("asnNo") String asnNo);

    /** 批量插入 */
    int batchInsert(@Param("list") List<CrossdockPreSort> list);
}
