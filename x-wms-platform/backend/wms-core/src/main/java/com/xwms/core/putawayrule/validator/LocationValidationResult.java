package com.xwms.core.putawayrule.validator;

import lombok.Data;

/** 库位校验结果 */
@Data
public class LocationValidationResult {

    /** 是否通过 */
    private boolean passed;

    /** 失败原因代码 */
    private String failCode;

    /** 失败原因描述 */
    private String failMessage;

    /** 校验维度：LOCATION_LIMIT库位限制/SPACE_LIMIT空间限制/EXTENDED_CONSTRAINT扩展约束 */
    private String dimension;

    public static LocationValidationResult pass() {
        LocationValidationResult result = new LocationValidationResult();
        result.setPassed(true);
        return result;
    }

    public static LocationValidationResult fail(
            String dimension, String failCode, String failMessage) {
        LocationValidationResult result = new LocationValidationResult();
        result.setPassed(false);
        result.setDimension(dimension);
        result.setFailCode(failCode);
        result.setFailMessage(failMessage);
        return result;
    }
}
