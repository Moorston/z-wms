package com.xwms.common.excel.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;

import com.xwms.common.excel.dto.ImportResult;
import com.xwms.common.excel.listener.BatchImportListener;

import lombok.extern.slf4j.Slf4j;

/** Excel导入导出工具类 基于EasyExcel，支持： 1. 同步导入（小文件） 2. 批量导入（大文件，分批处理） 3. 导出（单Sheet/多Sheet） 4. 下载模板 */
@Slf4j
public class ExcelUtil {

    /**
     * 同步导入Excel（小文件，全量加载到内存）
     *
     * @param file 上传文件
     * @param headClass 表头类（带@ExcelProperty注解）
     * @param validator 单行校验函数，返回null表示通过，返回错误信息表示失败
     * @param consumer 数据处理函数
     * @return 导入结果
     */
    public static <T> ImportResult<T> importSync(
            MultipartFile file,
            Class<T> headClass,
            Function<T, String> validator,
            Consumer<List<T>> consumer)
            throws IOException {
        long start = System.currentTimeMillis();
        BatchImportListener<T> listener = new BatchImportListener<>(100, validator, consumer);
        EasyExcel.read(file.getInputStream(), headClass, listener).sheet().doRead();
        ImportResult<T> result = listener.getResult();
        result.setCostMillis(System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 批量导入Excel（大文件，分批处理，避免OOM）
     *
     * @param file 上传文件
     * @param headClass 表头类
     * @param batchSize 批量大小
     * @param validator 单行校验函数
     * @param batchHandler 批量处理函数
     * @return 导入结果
     */
    public static <T> ImportResult<T> importBatch(
            MultipartFile file,
            Class<T> headClass,
            int batchSize,
            Function<T, String> validator,
            Consumer<List<T>> batchHandler)
            throws IOException {
        long start = System.currentTimeMillis();
        BatchImportListener<T> listener =
                new BatchImportListener<>(batchSize, validator, batchHandler);
        EasyExcel.read(file.getInputStream(), headClass, listener).sheet().doRead();
        ImportResult<T> result = listener.getResult();
        result.setCostMillis(System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 导出Excel到HttpServletResponse（单Sheet）
     *
     * @param response HTTP响应
     * @param fileName 文件名（不含扩展名）
     * @param sheetName Sheet名称
     * @param headClass 表头类
     * @param data 数据列表
     */
    public static <T> void export(
            HttpServletResponse response,
            String fileName,
            String sheetName,
            Class<T> headClass,
            List<T> data)
            throws IOException {
        setResponseHeader(response, fileName);
        EasyExcel.write(response.getOutputStream(), headClass).sheet(sheetName).doWrite(data);
    }

    /**
     * 导出Excel到HttpServletResponse（多Sheet）
     *
     * @param response HTTP响应
     * @param fileName 文件名
     * @param sheets 多Sheet配置，每个元素包含sheetName和data
     */
    @SafeVarargs
    public static <T> void exportMultiSheet(
            HttpServletResponse response, String fileName, SheetData<T>... sheets)
            throws IOException {
        setResponseHeader(response, fileName);
        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build()) {
            for (int i = 0; i < sheets.length; i++) {
                SheetData<T> sheet = sheets[i];
                WriteSheet writeSheet =
                        EasyExcel.writerSheet(i, sheet.getSheetName())
                                .head(sheet.getHeadClass())
                                .build();
                excelWriter.write(sheet.getData(), writeSheet);
            }
        }
    }

    /**
     * 下载导入模板
     *
     * @param response HTTP响应
     * @param fileName 文件名
     * @param headClass 表头类
     */
    public static <T> void downloadTemplate(
            HttpServletResponse response, String fileName, Class<T> headClass) throws IOException {
        setResponseHeader(response, fileName);
        EasyExcel.write(response.getOutputStream(), headClass).sheet("模板").doWrite(List.of());
    }

    /** 设置下载响应头 */
    private static void setResponseHeader(HttpServletResponse response, String fileName) {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName =
                URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader(
                "Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
    }

    /** 多Sheet数据封装 */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SheetData<T> {
        private String sheetName;
        private Class<T> headClass;
        private List<T> data;
    }
}
