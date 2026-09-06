package com.xwms.core.pallet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pallet.entity.PalletOperation;

/** 托盘操作记录Mapper */
@Mapper
public interface PalletOperationMapper extends BaseMapper<PalletOperation> {

    PalletOperation selectByOperationNo(@Param("operationNo") String operationNo);

    List<PalletOperation> selectByLpnNo(@Param("lpnNo") String lpnNo);

    List<PalletOperation> selectByOperationType(@Param("operationType") String operationType);

    List<PalletOperation> selectByAsnNo(@Param("asnNo") String asnNo);
}
