package com.xwms.core.returnorder.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.returnorder.entity.ReturnItem;

@Mapper
public interface ReturnItemMapper extends BaseMapper<ReturnItem> {

    @Select("SELECT * FROM wms_return_item WHERE return_id = #{returnId} ORDER BY id")
    List<ReturnItem> selectByReturnId(@Param("returnId") Long returnId);
}
