package com.xwms.core.serial.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.serial.entity.SerialNumber;

/** 序列号Mapper */
@Mapper
public interface SerialNumberMapper extends BaseMapper<SerialNumber> {

    /** 根据序列号查询 */
    SerialNumber selectBySerialNo(@Param("serialNo") String serialNo);

    /** 根据商品和仓库查询在库序列号 */
    List<SerialNumber> selectInStockBySku(
            @Param("skuCode") String skuCode, @Param("warehouseCode") String warehouseCode);

    /** 根据入库单查询序列号 */
    List<SerialNumber> selectByInboundNo(@Param("inboundNo") String inboundNo);

    /** 根据出库单查询序列号 */
    List<SerialNumber> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    /** 根据父序列号（箱号）查询子序列号 */
    List<SerialNumber> selectByParentSerialNo(@Param("parentSerialNo") String parentSerialNo);

    /** 批量插入 */
    int batchInsert(@Param("list") List<SerialNumber> list);

    /** 批量更新状态 */
    int batchUpdateStatus(
            @Param("serialNos") List<String> serialNos, @Param("status") String status);

    /** 检查序列号是否存在 */
    int countBySerialNo(@Param("serialNo") String serialNo);

    /** 检查序列号是否在当前仓库在库 */
    int countInStockInWarehouse(
            @Param("serialNo") String serialNo, @Param("warehouseCode") String warehouseCode);

    /** 检查序列号是否在当前ASN中 */
    int countInAsn(@Param("serialNo") String serialNo, @Param("asnNo") String asnNo);
}
