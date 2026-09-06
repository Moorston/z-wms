package com.xwms.core.config.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.config.entity.SysConfig;
import com.xwms.core.config.enums.ConfigCategory;
import com.xwms.core.config.mapper.SysConfigMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 系统参数配置服务 支持参数缓存（Redis+本地缓存）、参数初始化、仓库级/货主级参数覆盖 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 本地缓存（ConcurrentHashMap） */
    private final Map<String, SysConfig> localCache = new ConcurrentHashMap<>();

    /** Redis缓存前缀 */
    private static final String CACHE_PREFIX = "wms:config:";

    /** 缓存过期时间（小时） */
    private static final long CACHE_EXPIRE_HOURS = 24;

    // ==================== 23个入库关键参数常量 ====================

    /** ASN默认释放状态 */
    public static final String ASN_RLS_CTL = "ASN_RLS_CTL";

    /** 只有在预期到货时间范围内才能收货 */
    public static final String RCV_TIM_CTL = "RCV_TIM_CTL";

    /** PO默认释放状态 */
    public static final String PO_RLS_CTL = "PO_RLS_CTL";

    /** 序列号管理模式（0/1/2） */
    public static final String SN_CTL = "SN#_CTL";

    /** 入库序列号验证方式 */
    public static final String SN_RCV_VAL = "SN#_RCV_VAL";

    /** 收货前质检控制（Y/C/N） */
    public static final String QC_RCV_CTL = "QC_RCV_CTL";

    /** 上架质检控制 */
    public static final String QC_PTA_CTL = "QC_PTA_CTL";

    /** 收货后质检属性转移 */
    public static final String QC_FRM_TRN = "QC_FRM_TRN";

    /** 根据生产日期自动计算失效日期 */
    public static final String MDT_EDT_CAL = "MDT_EDT_CAL";

    /** 盲收模式（普通/简化） */
    public static final String RF_BRC_MOD = "RF_BRC_MOD";

    /** 整理收货模式 */
    public static final String RCV_AND_SRT = "RCV_AND_SRT";

    /** 快捷收货显示产品图片 */
    public static final String RCV_SHW_PIC = "RCV_SHW_PIC";

    /** 上架库位校验模式 */
    public static final String RF_PTA_CFM = "RF_PTA_CFM";

    /** 上架是否允许修改数量 */
    public static final String RF_PTA_CHG = "RF_PTA_CHG";

    /** 超大产品占用多库位控制 */
    public static final String CRS_LOC_CTL = "CRS_LOC_CTL";

    /** 上架采用逐件扫描模式 */
    public static final String PTA_PCS_SCN = "PTA_PCS_SCN";

    /** 通过扫描跟踪号和SKU获取上架任务 */
    public static final String PTA_SKU_CTL = "PTA_SKU_CTL";

    /** 在库包装处理方法 */
    public static final String PAC_CTL = "PAC_CTL";

    /** 是否允许跨仓移库 */
    public static final String MOV_BTW_WH = "MOV_BTW_WH";

    /** 订单类型是否与货主绑定 */
    public static final String CUS_ORD_LNK = "CUS_ORD_LNK";

    /** 收货是否允许混箱 */
    public static final String RCV_MIX_BOX = "RCV_MIX_BOX";

    /** 收货是否允许混ASN */
    public static final String RCV_MIX_ASN = "RCV_MIX_ASN";

    /** 上架是否允许覆盖库位 */
    public static final String PTA_LOC_OVR = "PTA_LOC_OVR";

    // ==================== 初始化方法 ====================

    /** 系统启动时初始化23个入库关键参数 */
    @PostConstruct
    public void initInboundConfigs() {
        log.info("开始初始化入库关键参数...");
        Map<String, SysConfig> inboundConfigs = buildInboundConfigs();
        for (Map.Entry<String, SysConfig> entry : inboundConfigs.entrySet()) {
            SysConfig existing = sysConfigMapper.selectByCode(entry.getKey());
            if (existing == null) {
                sysConfigMapper.insert(entry.getValue());
                log.info("初始化参数: {} = {}", entry.getKey(), entry.getValue().getDefaultValue());
            }
        }
        log.info("入库关键参数初始化完成，共{}个", inboundConfigs.size());
        // 刷新缓存
        refreshCache();
    }

    /** 构建23个入库关键参数定义 */
    private Map<String, SysConfig> buildInboundConfigs() {
        Map<String, SysConfig> configs = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // ASN_RLS_CTL
        configs.put(
                ASN_RLS_CTL,
                createConfig(
                        ASN_RLS_CTL,
                        "ASN默认释放状态",
                        "Y",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "asn",
                        "Y=已释放可收货/N=未释放需手动释放",
                        now));
        // RCV_TIM_CTL
        configs.put(
                RCV_TIM_CTL,
                createConfig(
                        RCV_TIM_CTL,
                        "预期到货时间范围控制",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "receipt",
                        "Y=仅在预期时间范围内可收货/N=不限制",
                        now));
        // PO_RLS_CTL
        configs.put(
                PO_RLS_CTL,
                createConfig(
                        PO_RLS_CTL,
                        "PO默认释放状态",
                        "Y",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "po",
                        "Y=已释放可提取/N=未释放需审核",
                        now));
        // SN#_CTL
        configs.put(
                SN_CTL,
                createConfig(
                        SN_CTL,
                        "序列号管理模式",
                        "0",
                        "NUMBER",
                        ConfigCategory.INBOUND.getCode(),
                        "serial",
                        "0=不管理/1=1级单品/2=2级箱+单品",
                        now));
        // SN#_RCV_VAL
        configs.put(
                SN_RCV_VAL,
                createConfig(
                        SN_RCV_VAL,
                        "入库序列号验证方式",
                        "1",
                        "NUMBER",
                        ConfigCategory.INBOUND.getCode(),
                        "serial",
                        "1=当前仓库/2=当前ASN/3=扫描队列",
                        now));
        // QC_RCV_CTL
        configs.put(
                QC_RCV_CTL,
                createConfig(
                        QC_RCV_CTL,
                        "收货前质检控制",
                        "N",
                        "STRING",
                        ConfigCategory.QC.getCode(),
                        "qc",
                        "Y=收货前质检/C=收货后质检/N=不质检",
                        now));
        // QC_PTA_CTL
        configs.put(
                QC_PTA_CTL,
                createConfig(
                        QC_PTA_CTL,
                        "上架质检控制",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.QC.getCode(),
                        "qc",
                        "Y=质检合格才能上架/N=不限制",
                        now));
        // QC_FRM_TRN
        configs.put(
                QC_FRM_TRN,
                createConfig(
                        QC_FRM_TRN,
                        "收货后质检属性转移",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.QC.getCode(),
                        "qc",
                        "Y=质检后同步转移批次属性/N=不转移",
                        now));
        // MDT_EDT_CAL
        configs.put(
                MDT_EDT_CAL,
                createConfig(
                        MDT_EDT_CAL,
                        "生产日期自动计算失效日期",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INVENTORY.getCode(),
                        "batch",
                        "Y=根据生产日期+保质期自动计算/N=手动录入",
                        now));
        // RF_BRC_MOD
        configs.put(
                RF_BRC_MOD,
                createConfig(
                        RF_BRC_MOD,
                        "盲收模式",
                        "NORMAL",
                        "STRING",
                        ConfigCategory.INBOUND.getCode(),
                        "receipt",
                        "NORMAL=普通模式/SIMPLE=简化模式",
                        now));
        // RCV_AND_SRT
        configs.put(
                RCV_AND_SRT,
                createConfig(
                        RCV_AND_SRT,
                        "整理收货模式",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "receipt",
                        "Y=开启整理收货(服装配比箱)/N=关闭",
                        now));
        // RCV_SHW_PIC
        configs.put(
                RCV_SHW_PIC,
                createConfig(
                        RCV_SHW_PIC,
                        "快捷收货显示产品图片",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "receipt",
                        "Y=显示产品图片/N=不显示",
                        now));
        // RF_PTA_CFM
        configs.put(
                RF_PTA_CFM,
                createConfig(
                        RF_PTA_CFM,
                        "上架库位校验模式",
                        "WARN",
                        "STRING",
                        ConfigCategory.INBOUND.getCode(),
                        "putaway",
                        "NONE=不校验/WARN=仅提示/FORCE=强制校验",
                        now));
        // RF_PTA_CHG
        configs.put(
                RF_PTA_CHG,
                createConfig(
                        RF_PTA_CHG,
                        "上架是否允许修改数量",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "putaway",
                        "Y=允许修改数量(分多次上架)/N=不允许",
                        now));
        // CRS_LOC_CTL
        configs.put(
                CRS_LOC_CTL,
                createConfig(
                        CRS_LOC_CTL,
                        "超大产品占用多库位控制",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.LOCATION.getCode(),
                        "location",
                        "Y=开启封存库位(超大产品)/N=关闭",
                        now));
        // PTA_PCS_SCN
        configs.put(
                PTA_PCS_SCN,
                createConfig(
                        PTA_PCS_SCN,
                        "上架逐件扫描模式",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "putaway",
                        "Y=逐件扫描上架/N=按数量上架",
                        now));
        // PTA_SKU_CTL
        configs.put(
                PTA_SKU_CTL,
                createConfig(
                        PTA_SKU_CTL,
                        "扫描跟踪号+SKU获取上架任务",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "putaway",
                        "Y=需同时扫描LPN和SKU/N=仅扫描LPN",
                        now));
        // PAC_CTL
        configs.put(
                PAC_CTL,
                createConfig(
                        PAC_CTL,
                        "在库包装处理方法",
                        "NONE",
                        "STRING",
                        ConfigCategory.INVENTORY.getCode(),
                        "consumable",
                        "NONE=不处理/DEDUCT=扣减耗材/RECORD=仅记录",
                        now));
        // MOV_BTW_WH
        configs.put(
                MOV_BTW_WH,
                createConfig(
                        MOV_BTW_WH,
                        "是否允许跨仓移库",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INVENTORY.getCode(),
                        "inventory",
                        "Y=允许跨仓移库/N=不允许",
                        now));
        // CUS_ORD_LNK
        configs.put(
                CUS_ORD_LNK,
                createConfig(
                        CUS_ORD_LNK,
                        "订单类型与货主绑定",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.SYSTEM.getCode(),
                        "system",
                        "Y=订单类型绑定货主/N=不绑定",
                        now));
        // RCV_MIX_BOX
        configs.put(
                RCV_MIX_BOX,
                createConfig(
                        RCV_MIX_BOX,
                        "收货是否允许混箱",
                        "Y",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "receipt",
                        "Y=允许混箱/N=不允许",
                        now));
        // RCV_MIX_ASN
        configs.put(
                RCV_MIX_ASN,
                createConfig(
                        RCV_MIX_ASN,
                        "收货是否允许混ASN",
                        "Y",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "receipt",
                        "Y=允许混ASN扫描/N=不允许",
                        now));
        // PTA_LOC_OVR
        configs.put(
                PTA_LOC_OVR,
                createConfig(
                        PTA_LOC_OVR,
                        "上架是否允许覆盖库位",
                        "N",
                        "BOOLEAN",
                        ConfigCategory.INBOUND.getCode(),
                        "putaway",
                        "Y=允许覆盖推荐库位(需原因)/N=不允许",
                        now));

        return configs;
    }

    private SysConfig createConfig(
            String code,
            String name,
            String defaultValue,
            String type,
            String category,
            String module,
            String desc,
            LocalDateTime now) {
        SysConfig config = new SysConfig();
        config.setConfigCode(code);
        config.setConfigName(name);
        config.setConfigValue(defaultValue);
        config.setDefaultValue(defaultValue);
        config.setConfigType(type);
        config.setCategory(category);
        config.setModuleCode(module);
        config.setEnabled("Y");
        config.setIsSystem("Y");
        config.setAllowModify("Y");
        config.setDescription(desc);
        config.setSortOrder(0);
        config.setCreatedBy("system");
        config.setCreatedTime(now);
        return config;
    }

    // ==================== 参数查询方法 ====================

    /** 获取参数值（全局） */
    public String getConfigValue(String configCode) {
        SysConfig config = getConfig(configCode);
        return config != null ? config.getConfigValue() : null;
    }

    /** 获取参数值（带默认值） */
    public String getConfigValue(String configCode, String defaultValue) {
        String value = getConfigValue(configCode);
        return value != null ? value : defaultValue;
    }

    /** 获取布尔参数 */
    public boolean getBooleanConfig(String configCode) {
        String value = getConfigValue(configCode);
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    /** 获取数字参数 */
    public Integer getIntConfig(String configCode) {
        String value = getConfigValue(configCode);
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            log.warn("参数{}不是数字类型: {}", configCode, value);
            return null;
        }
    }

    /** 获取参数对象（带缓存） */
    public SysConfig getConfig(String configCode) {
        // 1. 先查本地缓存
        SysConfig config = localCache.get(configCode);
        if (config != null) {
            return config;
        }
        // 2. 再查Redis缓存
        String cacheKey = CACHE_PREFIX + configCode;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof SysConfig) {
                config = (SysConfig) cached;
                localCache.put(configCode, config);
                return config;
            }
        } catch (Exception e) {
            log.warn("Redis获取参数缓存失败: {}", configCode, e);
        }
        // 3. 最后查数据库
        config = sysConfigMapper.selectByCode(configCode);
        if (config != null) {
            // 写入缓存
            localCache.put(configCode, config);
            try {
                redisTemplate
                        .opsForValue()
                        .set(cacheKey, config, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis写入参数缓存失败: {}", configCode, e);
            }
        }
        return config;
    }

    /** 获取仓库级参数（优先仓库级，其次全局） */
    public String getWarehouseConfig(String configCode, String warehouseCode) {
        SysConfig config = sysConfigMapper.selectByCodeAndWarehouse(configCode, warehouseCode);
        if (config != null) {
            return config.getConfigValue();
        }
        return getConfigValue(configCode);
    }

    /** 获取货主级参数（优先货主级，其次全局） */
    public String getOwnerConfig(String configCode, String ownerCode) {
        SysConfig config = sysConfigMapper.selectByCodeAndOwner(configCode, ownerCode);
        if (config != null) {
            return config.getConfigValue();
        }
        return getConfigValue(configCode);
    }

    // ==================== 参数管理方法 ====================

    /** 更新参数值 */
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigValue(String configCode, String configValue, String updatedBy) {
        SysConfig config = sysConfigMapper.selectByCode(configCode);
        if (config == null) {
            throw new RuntimeException("参数不存在: " + configCode);
        }
        if ("N".equals(config.getAllowModify())) {
            throw new RuntimeException("参数不允许修改: " + configCode);
        }
        // 校验参数值
        validateConfigValue(config, configValue);
        // 更新数据库
        sysConfigMapper.updateValue(configCode, configValue, updatedBy);
        // 清除缓存
        evictCache(configCode);
        log.info("参数更新: {} = {} (更新人: {})", configCode, configValue, updatedBy);
    }

    /** 校验参数值 */
    private void validateConfigValue(SysConfig config, String value) {
        if (value == null) {
            throw new RuntimeException("参数值不能为空");
        }
        String type = config.getConfigType();
        switch (type) {
            case "BOOLEAN":
                if (!"Y".equalsIgnoreCase(value)
                        && !"N".equalsIgnoreCase(value)
                        && !"true".equalsIgnoreCase(value)
                        && !"false".equalsIgnoreCase(value)) {
                    throw new RuntimeException("布尔参数值必须为Y/N或true/false");
                }
                break;
            case "NUMBER":
                try {
                    Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("数字参数值必须为有效数字");
                }
                break;
            default:
                break;
        }
    }

    /** 刷新所有缓存 */
    public void refreshCache() {
        log.info("开始刷新系统参数缓存...");
        List<SysConfig> configs = sysConfigMapper.selectAllEnabled();
        localCache.clear();
        for (SysConfig config : configs) {
            localCache.put(config.getConfigCode(), config);
            try {
                redisTemplate
                        .opsForValue()
                        .set(
                                CACHE_PREFIX + config.getConfigCode(),
                                config,
                                CACHE_EXPIRE_HOURS,
                                TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis写入参数缓存失败: {}", config.getConfigCode(), e);
            }
        }
        log.info("系统参数缓存刷新完成，共{}个参数", configs.size());
    }

    /** 清除指定参数缓存 */
    public void evictCache(String configCode) {
        localCache.remove(configCode);
        try {
            redisTemplate.delete(CACHE_PREFIX + configCode);
        } catch (Exception e) {
            log.warn("Redis删除参数缓存失败: {}", configCode, e);
        }
    }

    /** 按分类查询参数列表 */
    public List<SysConfig> getConfigsByCategory(String category) {
        return sysConfigMapper.selectByCategory(category);
    }

    /** 按模块查询参数列表 */
    public List<SysConfig> getConfigsByModule(String moduleCode) {
        return sysConfigMapper.selectByModule(moduleCode);
    }

    /** 查询所有参数 */
    public List<SysConfig> getAllConfigs() {
        return sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>()
                        .orderByAsc(SysConfig::getCategory, SysConfig::getSortOrder));
    }

    /** 重置参数为默认值 */
    @Transactional(rollbackFor = Exception.class)
    public void resetToDefault(String configCode, String updatedBy) {
        SysConfig config = sysConfigMapper.selectByCode(configCode);
        if (config == null) {
            throw new RuntimeException("参数不存在: " + configCode);
        }
        updateConfigValue(configCode, config.getDefaultValue(), updatedBy);
        log.info("参数重置为默认值: {} = {}", configCode, config.getDefaultValue());
    }
}
