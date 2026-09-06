package com.xwms.base.master.vo;

import java.math.BigDecimal;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;

import lombok.Data;

/** 商品导入导出VO 用于Excel批量导入导出商品档案 */
@Data
@HeadRowHeight(20)
@ContentRowHeight(18)
@ColumnWidth(20)
public class ProductExcelVO {

    @ExcelProperty(value = "SKU编码*", index = 0)
    @ColumnWidth(25)
    private String sku;

    @ExcelProperty(value = "商品名称*", index = 1)
    @ColumnWidth(30)
    private String productName;

    @ExcelProperty(value = "货主编码*", index = 2)
    private String ownerCode;

    @ExcelProperty(value = "条码", index = 3)
    private String barcode;

    @ExcelProperty(value = "分类", index = 4)
    private String category;

    @ExcelProperty(value = "品牌", index = 5)
    private String brand;

    @ExcelProperty(value = "规格", index = 6)
    private String spec;

    @ExcelProperty(value = "单位", index = 7)
    private String unit;

    @ExcelProperty(value = "重量(kg)", index = 8)
    private BigDecimal weight;

    @ExcelProperty(value = "体积(m³)", index = 9)
    private BigDecimal volume;

    @ExcelProperty(value = "温区", index = 10)
    private String temperatureZone;

    @ExcelProperty(value = "保质期(天)", index = 11)
    private Integer shelfLifeDays;

    @ExcelProperty(value = "批次管理(是/否)", index = 12)
    private String batchManaged;

    @ExcelProperty(value = "序列号管理(是/否)", index = 13)
    private String serialManaged;

    @ExcelProperty(value = "状态(启用/停用)", index = 14)
    private String status;
}
