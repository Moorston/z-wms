package com.xwms.common.excel.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import com.xwms.common.excel.dto.ImportResult;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 批量导入监听器 核心能力： 1. 分批读取（默认100条/批，避免OOM） 2. 错误收集（行号+字段+错误信息） 3. 数据校验（调用方提供校验逻辑） 4. 批量处理（调用方提供批量保存逻辑）
 *
 * @param <T> Excel行数据类型
 */
@Slf4j
public class BatchImportListener<T> extends AnalysisEventListener<T> {

    /** 批量大小 */
    private final int batchSize;

    /** 数据校验函数（可选） */
    private final java.util.function.Function<T, String> validator;

    /** 批量处理函数 */
    private final Consumer<List<T>> batchHandler;

    /** 导入结果 */
    @Getter private final ImportResult<T> result = new ImportResult<>();

    /** 当前批次数据 */
    private List<T> batchData = new ArrayList<>();

    /** 当前行号 */
    private int rowNum = 1; // 从1开始，第1行是表头

    public BatchImportListener(Consumer<List<T>> batchHandler) {
        this(100, null, batchHandler);
    }

    public BatchImportListener(
            int batchSize,
            java.util.function.Function<T, String> validator,
            Consumer<List<T>> batchHandler) {
        this.batchSize = batchSize;
        this.validator = validator;
        this.batchHandler = batchHandler;
    }

    @Override
    public void invoke(T data, AnalysisContext context) {
        rowNum++;
        result.setTotalCount(result.getTotalCount() + 1);

        // 1. 数据校验
        if (validator != null) {
            String errorMsg = validator.apply(data);
            if (errorMsg != null) {
                result.addError(rowNum, errorMsg);
                return;
            }
        }

        // 2. 加入批次
        batchData.add(data);

        // 3. 达到批量大小，处理
        if (batchData.size() >= batchSize) {
            processBatch();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理剩余数据
        if (!batchData.isEmpty()) {
            processBatch();
        }
        log.info(
                "导入完成: 总数={}, 成功={}, 失败={}",
                result.getTotalCount(),
                result.getSuccessCount(),
                result.getFailCount());
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("导入异常: row={}, error={}", rowNum, exception.getMessage());
        result.addError(rowNum, "数据格式错误: " + exception.getMessage());
    }

    /** 处理当前批次 */
    private void processBatch() {
        try {
            batchHandler.accept(batchData);
            batchData.forEach(result::addSuccess);
        } catch (Exception e) {
            log.error("批量处理失败: size={}, error={}", batchData.size(), e.getMessage());
            batchData.forEach(
                    d ->
                            result.addError(
                                    rowNum - batchData.size() + 1, "批量保存失败: " + e.getMessage()));
        }
        batchData = new ArrayList<>();
    }
}
