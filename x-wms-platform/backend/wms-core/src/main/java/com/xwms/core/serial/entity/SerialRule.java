package com.xwms.core.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 序列号规则表 定义序列号的长度、前缀、后缀、固定字符、合法性校验规则 */
@Data
@TableName("wms_serial_rule")
public class SerialRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则编码（唯一） */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 商品编码（为空表示通用规则） */
    private String skuCode;

    /** 货主编码（为空表示通用规则） */
    private String ownerCode;

    /** 序列号级别：1=单品级/2=箱级 */
    private Integer serialLevel;

    /** 序列号长度（0表示不限制） */
    private Integer length;

    /** 最小长度 */
    private Integer minLength;

    /** 最大长度 */
    private Integer maxLength;

    /** 前缀（多个用逗号分隔） */
    private String prefix;

    /** 后缀（多个用逗号分隔） */
    private String suffix;

    /** 中间固定字符（位置:字符，多个用逗号分隔，如3:X,5:Y） */
    private String fixedChars;

    /** 字符类型：ALPHA字母/NUMERIC数字/ALPHANUMERIC字母数字/ANY任意 */
    private String charType;

    /** 合法性校验布尔表达式（如 length()==20 && startsWith("8")） */
    private String validateExpression;

    /** 医药行业强制长度（16或20位） */
    private String medicalForceLength;

    /** 是否启用：Y/N */
    private String enabled;

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
