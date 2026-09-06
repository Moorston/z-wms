package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.AsnGroup;

/** ASN编组Mapper */
@Mapper
public interface AsnGroupMapper extends BaseMapper<AsnGroup> {

    /** 根据编组号查询 */
    AsnGroup selectByGroupNo(@Param("groupNo") String groupNo);

    /** 根据状态查询 */
    List<AsnGroup> selectByStatus(@Param("status") String status);

    /** 更新状态 */
    int updateStatus(@Param("groupNo") String groupNo, @Param("status") String status);
}
