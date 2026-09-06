package com.xwms.core.config.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.config.entity.SysConfig;
import com.xwms.core.config.service.SysConfigService;

import lombok.RequiredArgsConstructor;

/** 系统参数配置Controller */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    /** 获取所有参数 */
    @GetMapping("/list")
    public List<SysConfig> list() {
        return sysConfigService.getAllConfigs();
    }

    /** 按分类查询参数 */
    @GetMapping("/category/{category}")
    public List<SysConfig> getByCategory(@PathVariable String category) {
        return sysConfigService.getConfigsByCategory(category);
    }

    /** 按模块查询参数 */
    @GetMapping("/module/{moduleCode}")
    public List<SysConfig> getByModule(@PathVariable String moduleCode) {
        return sysConfigService.getConfigsByModule(moduleCode);
    }

    /** 获取参数值 */
    @GetMapping("/value/{configCode}")
    public Map<String, Object> getValue(@PathVariable String configCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("configCode", configCode);
        result.put("value", sysConfigService.getConfigValue(configCode));
        return result;
    }

    /** 获取参数详情 */
    @GetMapping("/{configCode}")
    public SysConfig getDetail(@PathVariable String configCode) {
        return sysConfigService.getConfig(configCode);
    }

    /** 更新参数值 */
    @PutMapping("/{configCode}")
    public Map<String, Object> updateValue(
            @PathVariable String configCode,
            @RequestParam String configValue,
            @RequestParam(required = false, defaultValue = "system") String updatedBy) {
        sysConfigService.updateConfigValue(configCode, configValue, updatedBy);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("configCode", configCode);
        result.put("newValue", configValue);
        return result;
    }

    /** 重置参数为默认值 */
    @PutMapping("/{configCode}/reset")
    public Map<String, Object> resetToDefault(
            @PathVariable String configCode,
            @RequestParam(required = false, defaultValue = "system") String updatedBy) {
        sysConfigService.resetToDefault(configCode, updatedBy);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("configCode", configCode);
        return result;
    }

    /** 刷新缓存 */
    @PostMapping("/refresh")
    public Map<String, Object> refreshCache() {
        sysConfigService.refreshCache();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "缓存刷新成功");
        return result;
    }

    /** 批量获取参数值 */
    @PostMapping("/batch")
    public Map<String, String> batchGetValues(@RequestBody List<String> configCodes) {
        Map<String, String> result = new HashMap<>();
        for (String code : configCodes) {
            result.put(code, sysConfigService.getConfigValue(code));
        }
        return result;
    }
}
