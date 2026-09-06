package com.xwms.core.plugin.industry.gsp.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.gsp.entity.GspLicense;

/** GSP证照管理Mapper */
@Mapper
public interface GspLicenseMapper extends BaseMapper<GspLicense> {

    /** 根据证照编号查询 */
    GspLicense selectByLicenseNo(@Param("licenseNo") String licenseNo);

    /** 根据证照类型查询 */
    List<GspLicense> selectByLicenseType(@Param("licenseType") String licenseType);

    /** 查询即将到期的证照 */
    List<GspLicense> selectExpiringLicenses(@Param("expiryDate") LocalDate expiryDate);

    /** 查询已过期的证照 */
    List<GspLicense> selectExpiredLicenses();

    /** 根据仓库编码查询 */
    List<GspLicense> selectByWarehouse(@Param("warehouseCode") String warehouseCode);

    /** 根据货主编码查询 */
    List<GspLicense> selectByOwner(@Param("ownerCode") String ownerCode);
}
