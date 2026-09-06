package com.xwms.core.pack.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.pack.entity.PackageEntity;

@Mapper
public interface PackageMapper extends BaseMapper<PackageEntity> {

    @Select("SELECT * FROM wms_package WHERE package_no = #{packageNo}")
    PackageEntity selectByPackageNo(@Param("packageNo") String packageNo);

    @Select("SELECT * FROM wms_package WHERE pack_no = #{packNo} ORDER BY created_time")
    List<PackageEntity> selectByPackNo(@Param("packNo") String packNo);

    @Select("SELECT * FROM wms_package WHERE outbound_no = #{outboundNo} ORDER BY created_time")
    List<PackageEntity> selectByOutboundNo(@Param("outboundNo") String outboundNo);

    @Select("SELECT * FROM wms_package WHERE tracking_no = #{trackingNo}")
    PackageEntity selectByTrackingNo(@Param("trackingNo") String trackingNo);
}
