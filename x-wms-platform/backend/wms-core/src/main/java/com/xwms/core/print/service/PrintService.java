package com.xwms.core.print.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.print.entity.*;
import com.xwms.core.print.enums.PrintTaskStatus;
import com.xwms.core.print.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 报表打印核心服务 包含: 打印模板/打印任务/打印机管理/打印队列 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintService {

    private final PrintTemplateMapper printTemplateMapper;
    private final PrintTaskMapper printTaskMapper;
    private final PrinterMapper printerMapper;
    private final PrintQueueMapper printQueueMapper;

    private static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    private static final AtomicInteger QUEUE_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 打印模板管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PrintTemplate createTemplate(PrintTemplate template) {
        printTemplateMapper.insert(template);
        log.info("创建打印模板: {}={}", template.getTemplateCode(), template.getTemplateName());
        return template;
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTemplate updateTemplate(PrintTemplate template) {
        printTemplateMapper.updateById(template);
        return template;
    }

    public Page<PrintTemplate> pageTemplates(
            Page<PrintTemplate> page, String templateType, Integer enabled) {
        LambdaQueryWrapper<PrintTemplate> wrapper = new LambdaQueryWrapper<>();
        if (templateType != null) wrapper.eq(PrintTemplate::getTemplateType, templateType);
        if (enabled != null) wrapper.eq(PrintTemplate::getEnabled, enabled);
        wrapper.orderByAsc(PrintTemplate::getTemplateType)
                .orderByAsc(PrintTemplate::getTemplateCode);
        return printTemplateMapper.selectPage(page, wrapper);
    }

    public List<PrintTemplate> getTemplatesByType(String templateType) {
        return printTemplateMapper.selectByType(templateType);
    }

    public PrintTemplate getTemplateByCode(String templateCode) {
        return printTemplateMapper.selectByCode(templateCode);
    }

    // ============================================================

    // 2. 打印任务
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PrintTask createPrintTask(
            String templateCode,
            String businessType,
            String businessNo,
            String printerCode,
            Integer copies,
            String printData,
            String createdBy) {
        PrintTemplate template = printTemplateMapper.selectByCode(templateCode);
        if (template == null) throw new RuntimeException("打印模板不存在: " + templateCode);

        // 确定打印机
        String actualPrinter = printerCode != null ? printerCode : template.getDefaultPrinter();
        Printer printer =
                actualPrinter != null
                        ? printerMapper.selectOne(
                                new LambdaQueryWrapper<Printer>()
                                        .eq(Printer::getPrinterCode, actualPrinter))
                        : null;

        PrintTask task = new PrintTask();
        task.setTaskNo(generateTaskNo());
        task.setTemplateCode(templateCode);
        task.setTemplateName(template.getTemplateName());
        task.setBusinessType(businessType);
        task.setBusinessNo(businessNo);
        task.setPrinterCode(actualPrinter);
        task.setPrinterName(printer != null ? printer.getPrinterName() : null);
        task.setCopies(copies != null ? copies : template.getCopies());
        task.setPrintData(printData);
        task.setStatus(PrintTaskStatus.PENDING.getCode());
        task.setCreatedBy(createdBy);
        printTaskMapper.insert(task);

        // 加入打印队列
        if (actualPrinter != null && printer != null && "ONLINE".equals(printer.getStatus())) {
            enqueueTask(task.getId(), task.getTaskNo(), actualPrinter, 5);
        }

        log.info("创建打印任务: {}, 模板={}, 打印机={}", task.getTaskNo(), templateCode, actualPrinter);
        return task;
    }

    public Page<PrintTask> pagePrintTasks(
            Page<PrintTask> page,
            String status,
            String businessType,
            String businessNo,
            String printerCode) {
        LambdaQueryWrapper<PrintTask> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(PrintTask::getStatus, status);
        if (businessType != null) wrapper.eq(PrintTask::getBusinessType, businessType);
        if (businessNo != null) wrapper.eq(PrintTask::getBusinessNo, businessNo);
        if (printerCode != null) wrapper.eq(PrintTask::getPrinterCode, printerCode);
        wrapper.orderByDesc(PrintTask::getCreatedTime);
        return printTaskMapper.selectPage(page, wrapper);
    }

    public List<PrintTask> getPrintTasksByBusiness(String businessType, String businessNo) {
        return printTaskMapper.selectByBusiness(businessType, businessNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTask cancelPrintTask(Long taskId) {
        PrintTask task = printTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("打印任务不存在");
        if (!PrintTaskStatus.PENDING.getCode().equals(task.getStatus())) {
            throw new RuntimeException("只有待打印任务可以取消");
        }
        task.setStatus(PrintTaskStatus.CANCELLED.getCode());
        printTaskMapper.updateById(task);
        log.info("取消打印任务: {}", task.getTaskNo());
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintTask retryPrintTask(Long taskId) {
        PrintTask task = printTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("打印任务不存在");
        if (!PrintTaskStatus.FAILED.getCode().equals(task.getStatus())) {
            throw new RuntimeException("只有失败任务可以重试");
        }
        task.setStatus(PrintTaskStatus.PENDING.getCode());
        task.setFailReason(null);
        printTaskMapper.updateById(task);

        // 重新入队
        if (task.getPrinterCode() != null) {
            enqueueTask(task.getId(), task.getTaskNo(), task.getPrinterCode(), 3);
        }
        log.info("重试打印任务: {}", task.getTaskNo());
        return task;
    }

    // ============================================================

    // 3. 打印机管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Printer createPrinter(Printer printer) {
        printerMapper.insert(printer);
        log.info("创建打印机: {}={}", printer.getPrinterCode(), printer.getPrinterName());
        return printer;
    }

    public Page<Printer> pagePrinters(Page<Printer> page, String warehouseCode, String status) {
        LambdaQueryWrapper<Printer> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(Printer::getWarehouseCode, warehouseCode);
        if (status != null) wrapper.eq(Printer::getStatus, status);
        wrapper.orderByAsc(Printer::getWarehouseCode).orderByAsc(Printer::getPrinterCode);
        return printerMapper.selectPage(page, wrapper);
    }

    public List<Printer> getPrintersByWarehouse(String warehouseCode) {
        return printerMapper.selectByWarehouse(warehouseCode);
    }

    public List<Printer> getOnlinePrinters() {
        return printerMapper.selectOnlinePrinters();
    }

    @Transactional(rollbackFor = Exception.class)
    public Printer updatePrinterStatus(String printerCode, String status) {
        Printer printer =
                printerMapper.selectOne(
                        new LambdaQueryWrapper<Printer>().eq(Printer::getPrinterCode, printerCode));
        if (printer == null) throw new RuntimeException("打印机不存在");
        printer.setStatus(status);
        printer.setLastHeartbeat(LocalDateTime.now());
        printerMapper.updateById(printer);
        log.info("更新打印机状态: {}={}", printerCode, status);
        return printer;
    }

    // ============================================================

    // 4. 打印队列
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PrintQueue enqueueTask(
            Long taskId, String taskNo, String printerCode, Integer priority) {
        PrintQueue queue = new PrintQueue();
        queue.setQueueNo(generateQueueNo());
        queue.setTaskId(taskId);
        queue.setTaskNo(taskNo);
        queue.setPrinterCode(printerCode);
        queue.setPriority(priority != null ? priority : 5);
        queue.setStatus("WAITING");
        queue.setRetryCount(0);
        queue.setMaxRetry(3);
        printQueueMapper.insert(queue);
        return queue;
    }

    public List<PrintQueue> getWaitingQueue(String printerCode) {
        return printQueueMapper.selectWaitingByPrinter(printerCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintQueue startPrint(Long queueId) {
        PrintQueue queue = printQueueMapper.selectById(queueId);
        if (queue == null) throw new RuntimeException("打印队列项不存在");
        queue.setStatus("PRINTING");
        queue.setStartTime(LocalDateTime.now());
        printQueueMapper.updateById(queue);

        // 更新任务状态
        PrintTask task = printTaskMapper.selectById(queue.getTaskId());
        if (task != null) {
            task.setStatus(PrintTaskStatus.PRINTING.getCode());
            printTaskMapper.updateById(task);
        }
        return queue;
    }

    @Transactional(rollbackFor = Exception.class)
    public PrintQueue finishPrint(Long queueId, boolean success, String failReason) {
        PrintQueue queue = printQueueMapper.selectById(queueId);
        if (queue == null) throw new RuntimeException("打印队列项不存在");
        queue.setStatus(success ? "DONE" : "FAILED");
        queue.setFinishTime(LocalDateTime.now());
        if (!success) {
            queue.setRetryCount(queue.getRetryCount() + 1);
        }
        printQueueMapper.updateById(queue);

        // 更新任务状态
        PrintTask task = printTaskMapper.selectById(queue.getTaskId());
        if (task != null) {
            if (success) {
                task.setStatus(PrintTaskStatus.SUCCESS.getCode());
                task.setPrintTime(LocalDateTime.now());
            } else if (queue.getRetryCount() >= queue.getMaxRetry()) {
                task.setStatus(PrintTaskStatus.FAILED.getCode());
                task.setFailReason(failReason);
            } else {
                // 重试: 重新入队
                task.setStatus(PrintTaskStatus.PENDING.getCode());
                enqueueTask(
                        task.getId(), task.getTaskNo(), task.getPrinterCode(), queue.getPriority());
            }
            printTaskMapper.updateById(task);
        }
        return queue;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateTaskNo() {
        return "PT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TASK_SEQ.incrementAndGet() % 1000);
    }

    private String generateQueueNo() {
        return "PQ"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", QUEUE_SEQ.incrementAndGet() % 1000);
    }
}
