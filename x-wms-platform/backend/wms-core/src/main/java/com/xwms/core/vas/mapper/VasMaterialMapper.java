package com.xwms.core.vas.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.vas.entity.VasMaterial;

@Mapper
public interface VasMaterialMapper extends BaseMapper<VasMaterial> {

    @Select("SELECT * FROM wms_vas_material WHERE order_id = #{orderId} ORDER BY id")
    List<VasMaterial> selectByOrderId(@Param("orderId") Long orderId);
}
