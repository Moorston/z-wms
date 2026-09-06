package com.xwms.core.datamart.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.datamart.entity.*;
import com.xwms.core.datamart.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryDataMartService {

    private final DataMartMapper dataMartMapper;
    private final MetricDefineMapper metricDefineMapper;
    private final DimensionDefineMapper dimensionDefineMapper;
    private final DataModelMapper dataModelMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 数据集市 ====================

    @Transactional(rollbackFor = Exception.class)
    public DataMart createDataMart(
            String martName,
            String martCode,
            String martType,
            String warehouseCode,
            String ownerCode,
            String description,
            String dataSource,
            String martConfig,
            String refreshStrategy,
            String refreshCron,
            Integer sortOrder,
            String createdBy) {
        DataMart mart = new DataMart();
        mart.setMartId(generateId("DM"));
        mart.setMartName(martName);
        mart.setMartCode(martCode);
        mart.setMartType(martType);
        mart.setWarehouseCode(warehouseCode);
        mart.setOwnerCode(ownerCode);
        mart.setDescription(description);
        mart.setDataSource(dataSource);
        mart.setMartConfig(martConfig);
        mart.setRefreshStrategy(refreshStrategy != null ? refreshStrategy : "SCHEDULED");
        mart.setRefreshCron(refreshCron);
        mart.setStatus("ACTIVE");
        mart.setIsActive("Y");
        mart.setSortOrder(sortOrder != null ? sortOrder : 100);
        mart.setCreatedBy(createdBy);
        dataMartMapper.insert(mart);
        log.info("创建数据集市: id={}, name={}, type={}", mart.getMartId(), martName, martType);
        return mart;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataMart refreshDataMart(
            String martId, Long recordCount, BigDecimal dataSizeMb, String operator) {
        DataMart mart = dataMartMapper.selectByMartId(martId);
        if (mart == null) throw new RuntimeException("数据集市不存在: " + martId);
        mart.setLastRefreshTime(LocalDateTime.now());
        mart.setRecordCount(recordCount);
        mart.setDataSizeMb(dataSizeMb);
        mart.setStatus("ACTIVE");
        dataMartMapper.updateById(mart);
        log.info("刷新数据集市: id={}, records={}, size={}MB", martId, recordCount, dataSizeMb);
        return dataMartMapper.selectByMartId(martId);
    }

    public List<DataMart> getActiveMartsByType(String martType) {
        return dataMartMapper.selectActiveByType(martType);
    }

    public List<DataMart> getActiveMartsByWarehouse(String warehouseCode) {
        return dataMartMapper.selectActiveByWarehouse(warehouseCode);
    }

    public Page<DataMart> pageDataMart(
            Page<DataMart> page,
            String martType,
            String warehouseCode,
            String status,
            String isActive) {
        LambdaQueryWrapper<DataMart> wrapper = new LambdaQueryWrapper<>();
        if (martType != null) wrapper.eq(DataMart::getMartType, martType);
        if (warehouseCode != null) wrapper.eq(DataMart::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(DataMart::getStatus, status);
        if (isActive != null) wrapper.eq(DataMart::getIsActive, isActive);
        wrapper.orderByAsc(DataMart::getSortOrder);
        return dataMartMapper.selectPage(page, wrapper);
    }

    // ==================== 指标管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public MetricDefine createMetricDefine(
            String metricName,
            String metricCode,
            String metricType,
            String metricCategory,
            String martId,
            String description,
            String calculationFormula,
            String dataSource,
            String unit,
            Integer precision,
            String aggregationType,
            String isDerived,
            String parentMetricId,
            BigDecimal targetValue,
            BigDecimal benchmarkValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            String direction,
            Integer sortOrder,
            String createdBy) {
        MetricDefine metric = new MetricDefine();
        metric.setMetricId(generateId("MD"));
        metric.setMetricName(metricName);
        metric.setMetricCode(metricCode);
        metric.setMetricType(metricType);
        metric.setMetricCategory(metricCategory);
        metric.setMartId(martId);
        metric.setDescription(description);
        metric.setCalculationFormula(calculationFormula);
        metric.setDataSource(dataSource);
        metric.setUnit(unit);
        metric.setPrecision(precision != null ? precision : 2);
        metric.setAggregationType(aggregationType != null ? aggregationType : "SUM");
        metric.setIsDerived(isDerived != null ? isDerived : "N");
        metric.setParentMetricId(parentMetricId);
        metric.setTargetValue(targetValue);
        metric.setBenchmarkValue(benchmarkValue);
        metric.setThresholdWarning(thresholdWarning);
        metric.setThresholdCritical(thresholdCritical);
        metric.setDirection(direction != null ? direction : "HIGHER");
        metric.setIsActive("Y");
        metric.setSortOrder(sortOrder != null ? sortOrder : 100);
        metric.setCreatedBy(createdBy);
        metricDefineMapper.insert(metric);
        log.info("创建指标定义: id={}, name={}, type={}", metric.getMetricId(), metricName, metricType);
        return metric;
    }

    public List<MetricDefine> getActiveMetricsByMart(String martId) {
        return metricDefineMapper.selectActiveByMart(martId);
    }

    public List<MetricDefine> getActiveMetricsByType(String metricType) {
        return metricDefineMapper.selectActiveByType(metricType);
    }

    public List<MetricDefine> getActiveMetricsByCategory(String metricCategory) {
        return metricDefineMapper.selectActiveByCategory(metricCategory);
    }

    public Page<MetricDefine> pageMetricDefine(
            Page<MetricDefine> page,
            String metricType,
            String metricCategory,
            String martId,
            String isActive) {
        LambdaQueryWrapper<MetricDefine> wrapper = new LambdaQueryWrapper<>();
        if (metricType != null) wrapper.eq(MetricDefine::getMetricType, metricType);
        if (metricCategory != null) wrapper.eq(MetricDefine::getMetricCategory, metricCategory);
        if (martId != null) wrapper.eq(MetricDefine::getMartId, martId);
        if (isActive != null) wrapper.eq(MetricDefine::getIsActive, isActive);
        wrapper.orderByAsc(MetricDefine::getMetricCategory).orderByAsc(MetricDefine::getSortOrder);
        return metricDefineMapper.selectPage(page, wrapper);
    }

    // ==================== 维度管理 ====================

    @Transactional(rollbackFor = Exception.class)
    public DimensionDefine createDimensionDefine(
            String dimensionName,
            String dimensionCode,
            String dimensionType,
            String martId,
            String description,
            String dataType,
            String dataSource,
            String dimensionConfig,
            Integer hierarchyLevel,
            String parentDimensionId,
            String isTimeDimension,
            String isGeoDimension,
            Integer sortOrder,
            String createdBy) {
        DimensionDefine dimension = new DimensionDefine();
        dimension.setDimensionId(generateId("DD"));
        dimension.setDimensionName(dimensionName);
        dimension.setDimensionCode(dimensionCode);
        dimension.setDimensionType(dimensionType);
        dimension.setMartId(martId);
        dimension.setDescription(description);
        dimension.setDataType(dataType != null ? dataType : "STRING");
        dimension.setDataSource(dataSource);
        dimension.setDimensionConfig(dimensionConfig);
        dimension.setHierarchyLevel(hierarchyLevel != null ? hierarchyLevel : 1);
        dimension.setParentDimensionId(parentDimensionId);
        dimension.setIsTimeDimension(isTimeDimension != null ? isTimeDimension : "N");
        dimension.setIsGeoDimension(isGeoDimension != null ? isGeoDimension : "N");
        dimension.setIsActive("Y");
        dimension.setSortOrder(sortOrder != null ? sortOrder : 100);
        dimension.setCreatedBy(createdBy);
        dimensionDefineMapper.insert(dimension);
        log.info(
                "创建维度定义: id={}, name={}, type={}",
                dimension.getDimensionId(),
                dimensionName,
                dimensionType);
        return dimension;
    }

    public List<DimensionDefine> getActiveDimensionsByMart(String martId) {
        return dimensionDefineMapper.selectActiveByMart(martId);
    }

    public List<DimensionDefine> getActiveDimensionsByType(String dimensionType) {
        return dimensionDefineMapper.selectActiveByType(dimensionType);
    }

    public List<DimensionDefine> getTimeDimensions() {
        return dimensionDefineMapper.selectTimeDimensions();
    }

    public Page<DimensionDefine> pageDimensionDefine(
            Page<DimensionDefine> page,
            String dimensionType,
            String martId,
            String isTimeDimension,
            String isActive) {
        LambdaQueryWrapper<DimensionDefine> wrapper = new LambdaQueryWrapper<>();
        if (dimensionType != null) wrapper.eq(DimensionDefine::getDimensionType, dimensionType);
        if (martId != null) wrapper.eq(DimensionDefine::getMartId, martId);
        if (isTimeDimension != null)
            wrapper.eq(DimensionDefine::getIsTimeDimension, isTimeDimension);
        if (isActive != null) wrapper.eq(DimensionDefine::getIsActive, isActive);
        wrapper.orderByAsc(DimensionDefine::getDimensionType)
                .orderByAsc(DimensionDefine::getSortOrder);
        return dimensionDefineMapper.selectPage(page, wrapper);
    }

    // ==================== 数据模型 ====================

    @Transactional(rollbackFor = Exception.class)
    public DataModel createDataModel(
            String modelName,
            String modelCode,
            String modelType,
            String martId,
            String description,
            String modelConfig,
            String tableName,
            String fields,
            String relations,
            String partitions,
            String indexes,
            String storageEngine,
            String refreshStrategy,
            String refreshCron,
            Integer sortOrder,
            String createdBy) {
        DataModel model = new DataModel();
        model.setModelId(generateId("DMO"));
        model.setModelName(modelName);
        model.setModelCode(modelCode);
        model.setModelType(modelType);
        model.setMartId(martId);
        model.setDescription(description);
        model.setModelConfig(modelConfig);
        model.setTableName(tableName);
        model.setFields(fields);
        model.setRelations(relations);
        model.setPartitions(partitions);
        model.setIndexes(indexes);
        model.setStorageEngine(storageEngine != null ? storageEngine : "OLAP");
        model.setRefreshStrategy(refreshStrategy != null ? refreshStrategy : "SCHEDULED");
        model.setRefreshCron(refreshCron);
        model.setStatus("ACTIVE");
        model.setIsActive("Y");
        model.setSortOrder(sortOrder != null ? sortOrder : 100);
        model.setCreatedBy(createdBy);
        dataModelMapper.insert(model);
        log.info(
                "创建数据模型: id={}, name={}, type={}, table={}",
                model.getModelId(),
                modelName,
                modelType,
                tableName);
        return model;
    }

    @Transactional(rollbackFor = Exception.class)
    public DataModel refreshDataModel(
            String modelId, Long recordCount, BigDecimal dataSizeMb, String operator) {
        DataModel model = dataModelMapper.selectByModelId(modelId);
        if (model == null) throw new RuntimeException("数据模型不存在: " + modelId);
        model.setLastRefreshTime(LocalDateTime.now());
        model.setRecordCount(recordCount);
        model.setDataSizeMb(dataSizeMb);
        model.setStatus("ACTIVE");
        dataModelMapper.updateById(model);
        log.info("刷新数据模型: id={}, records={}, size={}MB", modelId, recordCount, dataSizeMb);
        return dataModelMapper.selectByModelId(modelId);
    }

    public List<DataModel> getActiveModelsByMart(String martId) {
        return dataModelMapper.selectActiveByMart(martId);
    }

    public List<DataModel> getActiveModelsByType(String modelType) {
        return dataModelMapper.selectActiveByType(modelType);
    }

    public Page<DataModel> pageDataModel(
            Page<DataModel> page, String modelType, String martId, String status, String isActive) {
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        if (modelType != null) wrapper.eq(DataModel::getModelType, modelType);
        if (martId != null) wrapper.eq(DataModel::getMartId, martId);
        if (status != null) wrapper.eq(DataModel::getStatus, status);
        if (isActive != null) wrapper.eq(DataModel::getIsActive, isActive);
        wrapper.orderByAsc(DataModel::getModelType).orderByAsc(DataModel::getSortOrder);
        return dataModelMapper.selectPage(page, wrapper);
    }

    private String generateId(String prefix) {
        return prefix
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
