package com.xwms.core.plugin.industry.gsp.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** GSP证照管理 药品经营质量管理规范(GSP)相关证照管理 */
@Data
@TableName("wms_gsp_license")
public class GspLicense {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 证照编号 */
    private String licenseNo;

    /** 证照名称 */
    private String licenseName;

    /**
     * 证照类型: GSP_CERTIFICATE
     * GSP认证证书/DRUG_LICENSE药品经营许可证/BUSINESS_LICENSE营业执照/TAX_REGISTRATION税务登记证/ORGANIZATION_CODE组织机构代码/HEALTH_PERMIT卫生许可证/OTHER其他
     */
    private String licenseType;

    /** 证照编号(官方编号) */
    private String certificateNo;

    /** 持证单位名称 */
    private String holderName;

    /** 法定代表人 */
    private String legalRepresentative;

    /** 企业负责人 */
    private String enterprisePrincipal;

    /** 质量负责人 */
    private String qualityPrincipal;

    /** 注册地址 */
    private String registeredAddress;

    /** 经营地址 */
    private String businessAddress;

    /** 仓库地址 */
    private String warehouseAddress;

    /** 经营范围 */
    private String businessScope;

    /** 经营方式: RETAIL零售/WHOLESALE批发/RETAIL_CHAIN零售连锁 */
    private String businessMode;

    /** 发证机关 */
    private String issuingAuthority;

    /** 发证日期 */
    private LocalDate issueDate;

    /** 有效期至 */
    private LocalDate expiryDate;

    /** 换证日期 */
    private LocalDate renewalDate;

    /** 证照状态: ACTIVE有效/EXPIRING即将到期/EXPIRED已过期/REVOKED已吊销/CANCELLED已注销 */
    private String status;

    /** 预警天数(提前多少天预警) */
    private Integer warningDays;

    /** 是否已预警: Y是/N否 */
    private String isWarned;

    /** 预警时间 */
    private LocalDateTime warnTime;

    /** 关联仓库编码 */
    private String warehouseCode;

    /** 关联货主编码 */
    private String ownerCode;

    /** 关联供应商编码 */
    private String supplierCode;

    /** 证照扫描件URL */
    private String scanFileUrl;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
