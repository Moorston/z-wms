package com.xwms.core.transfer.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.transfer.entity.TransferDetail;

@Mapper
public interface TransferDetailMapper extends BaseMapper<TransferDetail> {

    @Select("SELECT * FROM wms_transfer_detail WHERE transfer_no = #{transferNo} ORDER BY line_no")
    List<TransferDetail> selectByTransferNo(@Param("transferNo") String transferNo);
}
