package com.xwms.common.config;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.xwms.common.core.Result;
import com.xwms.common.exception.BizException;
import com.xwms.common.i18n.I18nUtil;

import lombok.extern.slf4j.Slf4j;

/** 全局异常处理器 统一异常响应格式，支持国际化消息 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（支持国际化） */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        // 使用国际化消息
        String message = e.getI18nMessage();
        log.warn(
                "业务异常: code={}, msg={}, locale={}",
                e.getCode(),
                message,
                I18nUtil.getCurrentLocale());
        return Result.error(e.getCode(), message);
    }

    /** 参数校验异常 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse(I18nUtil.getMessage("common.invalid_param"));
        log.warn("参数校验失败: {}", msg);
        return Result.error(400, msg);
    }

    /** 绑定异常 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        return Result.error(
                400, I18nUtil.getMessage("common.invalid_param") + ": " + e.getMessage());
    }

    /** 非法参数 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }

    /** 未授权异常 */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(403, I18nUtil.getMessage("common.forbidden"));
    }

    /** 系统异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, I18nUtil.getMessage("common.server_error"));
    }
}
