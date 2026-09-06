package com.xwms.base.dict.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.base.dict.entity.DictItem;
import com.xwms.base.dict.entity.DictType;
import com.xwms.base.dict.service.DictService;
import com.xwms.common.core.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 数据字典Controller
 *
 * <p>提供字典类型和字典项的CRUD，以及字典翻译接口
 */
@Tag(name = "数据字典管理", description = "字典类型、字典项的CRUD及翻译")
@RestController
@RequestMapping("/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ==================== 字典类型 ====================

    @Operation(summary = "查询所有字典类型")
    @GetMapping("/types")
    public Result<List<DictType>> listTypes() {
        return Result.success(dictService.listTypes());
    }

    @Operation(summary = "新增字典类型")
    @PostMapping("/types")
    public Result<DictType> createType(@RequestBody DictType type) {
        return Result.success(dictService.createType(type));
    }

    @Operation(summary = "更新字典类型")
    @PutMapping("/types")
    public Result<Void> updateType(@RequestBody DictType type) {
        dictService.updateType(type);
        return Result.success();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Result.success();
    }

    // ==================== 字典项 ====================

    @Operation(summary = "根据字典类型编码查询字典项列表")
    @GetMapping("/items/{dictCode}")
    public Result<List<DictItem>> listItems(@PathVariable String dictCode) {
        return Result.success(dictService.listItems(dictCode));
    }

    @Operation(summary = "级联查询子字典项")
    @GetMapping("/items/{dictCode}/{parentValue}")
    public Result<List<DictItem>> listItemsByParent(
            @PathVariable String dictCode, @PathVariable String parentValue) {
        return Result.success(dictService.listItemsByParent(dictCode, parentValue));
    }

    @Operation(summary = "新增字典项")
    @PostMapping("/items")
    public Result<DictItem> createItem(@RequestBody DictItem item) {
        return Result.success(dictService.createItem(item));
    }

    @Operation(summary = "更新字典项")
    @PutMapping("/items")
    public Result<Void> updateItem(@RequestBody DictItem item) {
        dictService.updateItem(item);
        return Result.success();
    }

    @Operation(summary = "删除字典项")
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dictService.deleteItem(id);
        return Result.success();
    }

    // ==================== 字典翻译 ====================

    @Operation(summary = "字典翻译（value → label）")
    @GetMapping("/translate/{dictCode}/{itemValue}")
    public Result<String> translate(@PathVariable String dictCode, @PathVariable String itemValue) {
        return Result.success(dictService.translate(dictCode, itemValue));
    }

    @Operation(summary = "批量翻译（返回Map）")
    @GetMapping("/translate-map/{dictCode}")
    public Result<Map<String, String>> translateMap(@PathVariable String dictCode) {
        return Result.success(dictService.translateMap(dictCode));
    }

    @Operation(summary = "反向翻译（label → value）")
    @GetMapping("/reverse-translate/{dictCode}/{itemLabel}")
    public Result<String> reverseTranslate(
            @PathVariable String dictCode, @PathVariable String itemLabel) {
        return Result.success(dictService.reverseTranslate(dictCode, itemLabel));
    }

    // ==================== 缓存管理 ====================

    @Operation(summary = "刷新字典缓存")
    @PostMapping("/cache/refresh")
    public Result<Void> refreshCache() {
        dictService.refreshAllCache();
        return Result.success();
    }

    @Operation(summary = "清除指定字典缓存")
    @DeleteMapping("/cache/{dictCode}")
    public Result<Void> evictCache(@PathVariable String dictCode) {
        dictService.evictCache(dictCode);
        return Result.success();
    }
}
