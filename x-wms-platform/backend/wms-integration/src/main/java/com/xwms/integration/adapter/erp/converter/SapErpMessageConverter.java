package com.xwms.integration.adapter.erp.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.integration.adapter.erp.config.SapErpConfig;
import com.xwms.integration.adapter.erp.dto.SapErpDto;
import com.xwms.integration.adapter.erp.dto.SapErpFieldMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WMS DTO ↔ SAP OData 字段映射转换器
 *
 * <p>集中承载所有字段映射逻辑，业务层（Adapter）只调用本类、不关心 SAP 字段名。 修改字段映射只需改本类，Adapter 无需改动。
 *
 * <p>设计要点：
 *
 * <ul>
 *   <li>SAP 日期格式为 YYYYMMDD（无分隔符），与 Java {@link LocalDate} 双向转换
 *   <li>SAP 数量字段为字符串（保留小数位），从 BigDecimal 转换时统一格式化为 "xxx.000"
 *   <li>必填字段缺失时抛 {@link IllegalArgumentException}，由上层统一处理
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SapErpMessageConverter {

    private final SapErpConfig sapErpConfig;

    private static final DateTimeFormatter SAP_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int QUANTITY_SCALE = 3; // SAP 数量默认 3 位小数

    // ============================================================
    // I1 入库过账 → SAP
    // ============================================================

    public SapErpDto.GoodsMovementPayload toInboundPayload(SapErpDto.InboundPostRequest request) {
        validateNotBlank("inboundNo", request.getInboundNo());
        validateNotBlank("items", String.valueOf(request.getItems()));
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("入库过账明细不能为空");
        }

        Map<String, Object> header = new LinkedHashMap<>();
        header.put(SapErpFieldMapping.F_MVMT_TYPE, SapErpFieldMapping.MVMT_TYPE_INBOUND_PURCHASE);
        header.put(SapErpFieldMapping.F_MVMT_REASON, SapErpFieldMapping.MVMT_REASON_INBOUND);
        header.put(SapErpFieldMapping.F_POSTNG_DATE, formatDate(request.getPostingDate()));
        header.put(SapErpFieldMapping.F_DOC_DATE, formatDate(request.getDocDate()));
        header.put(
                SapErpFieldMapping.F_REF_DOC_NO,
                defaultIfBlank(request.getRefDocument(), request.getInboundNo()));
        header.put(
                SapErpFieldMapping.F_RPT_KEY_FLD,
                defaultIfBlank(request.getRptKeyField(), SapErpFieldMapping.DEFAULT_RPT_KEY_FLD));
        header.put(
                SapErpFieldMapping.F_PLANT,
                defaultIfBlank(request.getPlant(), sapErpConfig.getPlant()));
        header.put(
                SapErpFieldMapping.F_STGE_LOC,
                defaultIfBlank(request.getStorageLocation(), sapErpConfig.getStorageLocation()));

        List<Map<String, Object>> items = new ArrayList<>();
        for (SapErpDto.GoodsMovementItem item : request.getItems()) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put(SapErpFieldMapping.F_MATERIAL, item.getMaterial());
            if (item.getBatch() != null) {
                itemMap.put(SapErpFieldMapping.F_BATCH, item.getBatch());
            }
            itemMap.put(SapErpFieldMapping.F_MVMT_QUANT, formatQuantity(item.getMvmtQuantity()));
            itemMap.put(
                    SapErpFieldMapping.F_ENTRY_UOM,
                    defaultIfBlank(item.getEntryUom(), SapErpFieldMapping.DEFAULT_UOM));
            if (item.getPoNumber() != null || request.getPoNumber() != null) {
                itemMap.put(
                        SapErpFieldMapping.F_PURCH_DOC,
                        defaultIfBlank(request.getPoNumber(), item.getPoNumber()));
            }
            if (item.getPoItem() != null) {
                itemMap.put(SapErpFieldMapping.F_PURCH_DOC_IT, item.getPoItem());
            }
            itemMap.put(
                    SapErpFieldMapping.F_STGE_LOC,
                    defaultIfBlank(item.getTargetLocation(), sapErpConfig.getStorageLocation()));
            if (item.getSpecialStockType() != null) {
                itemMap.put(SapErpFieldMapping.F_SPL_STOCK, item.getSpecialStockType());
            }
            if (item.getSpecialStockNo() != null) {
                itemMap.put(SapErpFieldMapping.F_SPL_STOCK_NO, item.getSpecialStockNo());
            }
            items.add(itemMap);
        }

        SapErpDto.GoodsMovementPayload payload = new SapErpDto.GoodsMovementPayload();
        payload.setGoodsMvtHeader(header);
        payload.setGoodsMvtItem(items);
        return payload;
    }

    // ============================================================
    // I2 出库过账 → SAP
    // ============================================================

    public SapErpDto.GoodsMovementPayload toOutboundPayload(SapErpDto.OutboundPostRequest request) {
        validateNotBlank("shipmentNo", request.getShipmentNo());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("出库过账明细不能为空");
        }

        Map<String, Object> header = new LinkedHashMap<>();
        header.put(SapErpFieldMapping.F_MVMT_TYPE, SapErpFieldMapping.MVMT_TYPE_OUTBOUND_SALES);
        header.put(SapErpFieldMapping.F_MVMT_REASON, SapErpFieldMapping.MVMT_REASON_OUTBOUND);
        header.put(SapErpFieldMapping.F_POSTNG_DATE, formatDate(request.getPostingDate()));
        header.put(SapErpFieldMapping.F_DOC_DATE, formatDate(request.getDocDate()));
        header.put(
                SapErpFieldMapping.F_REF_DOC_NO,
                defaultIfBlank(request.getRefDocument(), request.getShipmentNo()));
        header.put(
                SapErpFieldMapping.F_RPT_KEY_FLD,
                defaultIfBlank(request.getRptKeyField(), SapErpFieldMapping.DEFAULT_RPT_KEY_FLD));
        header.put(
                SapErpFieldMapping.F_PLANT,
                defaultIfBlank(request.getPlant(), sapErpConfig.getPlant()));
        header.put(
                SapErpFieldMapping.F_STGE_LOC,
                defaultIfBlank(request.getStorageLocation(), sapErpConfig.getStorageLocation()));

        List<Map<String, Object>> items = new ArrayList<>();
        for (SapErpDto.GoodsMovementItem item : request.getItems()) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put(SapErpFieldMapping.F_MATERIAL, item.getMaterial());
            if (item.getBatch() != null) {
                itemMap.put(SapErpFieldMapping.F_BATCH, item.getBatch());
            }
            itemMap.put(SapErpFieldMapping.F_MVMT_QUANT, formatQuantity(item.getMvmtQuantity()));
            itemMap.put(
                    SapErpFieldMapping.F_ENTRY_UOM,
                    defaultIfBlank(item.getEntryUom(), SapErpFieldMapping.DEFAULT_UOM));
            if (request.getSalesOrder() != null) {
                itemMap.put(SapErpFieldMapping.F_SALES_ORD, request.getSalesOrder());
            }
            if (item.getSalesOrderItem() != null) {
                itemMap.put(SapErpFieldMapping.F_SALES_ORD_IT, item.getSalesOrderItem());
            }
            itemMap.put(
                    SapErpFieldMapping.F_STGE_LOC,
                    defaultIfBlank(item.getTargetLocation(), sapErpConfig.getStorageLocation()));
            if (item.getSpecialStockType() != null) {
                itemMap.put(SapErpFieldMapping.F_SPL_STOCK, item.getSpecialStockType());
            }
            if (item.getSpecialStockNo() != null) {
                itemMap.put(SapErpFieldMapping.F_SPL_STOCK_NO, item.getSpecialStockNo());
            }
            items.add(itemMap);
        }

        SapErpDto.GoodsMovementPayload payload = new SapErpDto.GoodsMovementPayload();
        payload.setGoodsMvtHeader(header);
        payload.setGoodsMvtItem(items);
        return payload;
    }

    // ============================================================
    // I3 库存同步 → SAP
    // ============================================================

    public SapErpDto.InventoryPayload toInventoryPayload(SapErpDto.InventorySyncRequest request) {
        validateNotBlank("material", request.getMaterial());
        validateNotBlank("stockQty", String.valueOf(request.getStockQty()));

        Map<String, Object> stock = new LinkedHashMap<>();
        stock.put(SapErpFieldMapping.F_MATNR, request.getMaterial());
        stock.put(
                SapErpFieldMapping.F_WERKS,
                defaultIfBlank(request.getPlant(), sapErpConfig.getPlant()));
        stock.put(
                SapErpFieldMapping.F_LGORT,
                defaultIfBlank(request.getStorageLocation(), sapErpConfig.getStorageLocation()));
        if (request.getBatch() != null) {
            stock.put(SapErpFieldMapping.F_CHARG, request.getBatch());
        }
        stock.put(SapErpFieldMapping.F_LBKUM, formatQuantity(request.getStockQty()));
        stock.put(
                SapErpFieldMapping.F_LEHMEN,
                defaultIfBlank(request.getStockUom(), SapErpFieldMapping.DEFAULT_UOM));
        stock.put(
                SapErpFieldMapping.F_LABOR,
                formatQuantity(defaultZero(request.getUnrestrictedUseStockQty())));
        stock.put(
                SapErpFieldMapping.F_QLMNG,
                formatQuantity(defaultZero(request.getQualityInspectionStockQty())));
        stock.put(
                SapErpFieldMapping.F_SPMNG,
                formatQuantity(defaultZero(request.getBlockedStockQty())));

        SapErpDto.InventoryPayload payload = new SapErpDto.InventoryPayload();
        payload.setStockInTransit(stock);
        return payload;
    }

    // ============================================================
    // I4 物料主数据 SAP → WMS
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<SapErpDto.MaterialMasterItem> toMaterialMasterItems(
            Map<String, Object> sapResponse) {
        List<SapErpDto.MaterialMasterItem> result = new ArrayList<>();
        if (sapResponse == null) return result;

        // SAP OData V2 响应结构: { "d": { "results": [ ... ] } }
        Object dObj = sapResponse.get("d");
        if (!(dObj instanceof Map)) return result;
        Object resultsObj = ((Map<String, Object>) dObj).get("results");
        if (!(resultsObj instanceof List)) return result;

        for (Object obj : (List<Object>) resultsObj) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> row = (Map<String, Object>) obj;

            SapErpDto.MaterialMasterItem item = new SapErpDto.MaterialMasterItem();
            item.setSkuCode(getString(row, SapErpFieldMapping.F_MM_MATERIAL));
            item.setSkuName(getString(row, SapErpFieldMapping.F_MM_DESC));
            item.setMaterialType(getString(row, SapErpFieldMapping.F_MM_TYPE));
            item.setCategoryCode(getString(row, SapErpFieldMapping.F_MM_GROUP));
            item.setUnitCode(getString(row, SapErpFieldMapping.F_MM_UNIT));
            item.setNetWeight(getDecimal(row, SapErpFieldMapping.F_MM_NET_WEIGHT));
            item.setGrossWeight(getDecimal(row, SapErpFieldMapping.F_MM_GROSS_WEIGHT));
            item.setWeightUom(getString(row, SapErpFieldMapping.F_MM_WEIGHT_UOM));
            item.setLength(getDecimal(row, SapErpFieldMapping.F_MM_LENGTH));
            item.setWidth(getDecimal(row, SapErpFieldMapping.F_MM_WIDTH));
            item.setHeight(getDecimal(row, SapErpFieldMapping.F_MM_HEIGHT));
            item.setDimensionUom(getString(row, SapErpFieldMapping.F_MM_DIM_UOM));
            item.setExternalCode(getString(row, SapErpFieldMapping.F_MM_EXT_NO));
            item.setCreatedTime(parseSapDate(getString(row, SapErpFieldMapping.F_MM_CREATED)));
            item.setActive(parseBool(getString(row, SapErpFieldMapping.F_MM_ACTIVE)));
            result.add(item);
        }
        return result;
    }

    // ============================================================
    // I5 仓库/库位主数据 SAP → WMS
    // ============================================================

    @SuppressWarnings("unchecked")
    public List<SapErpDto.WarehouseMasterItem> toWarehouseMasterItems(
            Map<String, Object> sapResponse) {
        List<SapErpDto.WarehouseMasterItem> result = new ArrayList<>();
        if (sapResponse == null) return result;

        Object dObj = sapResponse.get("d");
        if (!(dObj instanceof Map)) return result;
        Object resultsObj = ((Map<String, Object>) dObj).get("results");
        if (!(resultsObj instanceof List)) return result;

        for (Object obj : (List<Object>) resultsObj) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> row = (Map<String, Object>) obj;

            SapErpDto.WarehouseMasterItem item = new SapErpDto.WarehouseMasterItem();
            item.setWarehouseCode(getString(row, SapErpFieldMapping.F_WH_PLANT));
            item.setWarehouseName(getString(row, SapErpFieldMapping.F_WH_PLANT_NAME));
            item.setStorageAreaCode(getString(row, SapErpFieldMapping.F_WH_STGE_LOC));
            item.setStorageAreaName(getString(row, SapErpFieldMapping.F_WH_STGE_LOC_NAME));
            item.setStorageType(getString(row, SapErpFieldMapping.F_WH_STGE_TYPE));
            item.setActive(parseBool(getString(row, "IsActive")));
            result.add(item);
        }
        return result;
    }

    // ============================================================
    // 响应解析：从 SAP 过账响应中提取凭证号
    // ============================================================

    @SuppressWarnings("unchecked")
    public String extractDocNum(Map<String, Object> sapResponse) {
        if (sapResponse == null) return null;
        Object dObj = sapResponse.get("d");
        if (!(dObj instanceof Map)) return null;
        Object headerObj = ((Map<String, Object>) dObj).get("MVMT_HEADER");
        if (!(headerObj instanceof Map)) return null;
        Map<String, Object> headerMap = (Map<String, Object>) headerObj;
        Object docNum = headerMap.get("DocNum");
        return docNum != null ? docNum.toString() : null;
    }

    // ============================================================
    // 错误消息提取（从 SAP 错误响应中解析）
    // ============================================================

    @SuppressWarnings("unchecked")
    public String extractErrorMessage(Map<String, Object> sapError) {
        if (sapError == null) return null;
        Object errObj = sapError.get("error");
        if (!(errObj instanceof Map)) return null;
        Map<String, Object> err = (Map<String, Object>) errObj;
        Object msgObj = err.get("message");
        if (msgObj instanceof Map) {
            Object value = ((Map<String, Object>) msgObj).get("value");
            if (value != null) return value.toString();
        }
        return errObj.toString();
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private void validateNotBlank(String field, Object value) {
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("字段缺失或为空: " + field);
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) return LocalDate.now().format(SAP_DATE_FMT);
        return date.format(SAP_DATE_FMT);
    }

    private String formatQuantity(BigDecimal qty) {
        if (qty == null) return String.format("%" + QUANTITY_SCALE + "f", 0.0).replace('.', '.');
        return qty.setScale(QUANTITY_SCALE, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private BigDecimal getDecimal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException e) {
            log.warn("无法解析数量: key={}, value={}", key, v);
            return null;
        }
    }

    private Boolean parseBool(String value) {
        if (value == null) return null;
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "X".equals(value);
    }

    /** 解析 SAP 日期格式：/Date(1725014400000)/ 或 ISO 格式 */
    private LocalDate parseSapDate(String value) {
        if (value == null || value.isBlank()) return null;
        // SAP OData 日期: /Date(1725014400000)/
        if (value.startsWith("/Date(") && value.endsWith(")/")) {
            String digits = value.substring(6, value.length() - 2);
            try {
                long ts = Long.parseLong(digits);
                return LocalDate.ofInstant(
                        java.time.Instant.ofEpochMilli(ts), java.time.ZoneId.systemDefault());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return LocalDate.parse(value, ISO_DATE_FMT);
        } catch (Exception e) {
            log.warn("无法解析日期: {}", value);
            return null;
        }
    }
}
