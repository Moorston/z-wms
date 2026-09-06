package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.ReturnOrderDetail;

/** 退货单明细Mapper */
@Mapper
public interface ReturnOrderDetailMapper extends BaseMapper<ReturnOrderDetail> {

    /** 根据退货单号查询明细 */
    List<ReturnOrderDetail> selectByReturnNo(@Param("returnNo") String returnNo);

    /** 根据退货单号和商品编码查询 */
    ReturnOrderDetail selectByReturnNoAndSku(
            @Param("returnNo") String returnNo, @Param("skuCode") String skuCode);

    /** 批量插入 */
    int batchInsert(@Param("list") List<ReturnOrderDetail> list);
}
