package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.AsnGroupDetail;

/** ASN编组明细Mapper */
@Mapper
public interface AsnGroupDetailMapper extends BaseMapper<AsnGroupDetail> {

    /** 根据编组号查询明细 */
    List<AsnGroupDetail> selectByGroupNo(@Param("groupNo") String groupNo);

    /** 根据编组号和商品编码查询 */
    List<AsnGroupDetail> selectByGroupNoAndSku(
            @Param("groupNo") String groupNo, @Param("skuCode") String skuCode);

    /** 批量插入 */
    int batchInsert(@Param("list") List<AsnGroupDetail> list);
}
