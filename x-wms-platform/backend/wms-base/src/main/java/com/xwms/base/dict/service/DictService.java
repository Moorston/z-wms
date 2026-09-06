package com.xwms.base.dict.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.dict.entity.DictItem;
import com.xwms.base.dict.entity.DictType;
import com.xwms.base.dict.mapper.DictItemMapper;
import com.xwms.base.dict.mapper.DictTypeMapper;
import com.xwms.common.exception.BizException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据字典服务
 *
 * <p>核心能力： 1. 字典类型CRUD 2. 字典项CRUD 3. 字典项查询（Redis缓存，提高性能） 4. 字典翻译（itemValue → itemLabel）
 *
 * <p>缓存策略： - 缓存key: wms:dict:items:{dictCode} - 缓存值: List<DictItem> JSON - 过期时间: 24小时 - 变更时主动清除缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private static final String CACHE_PREFIX = "wms:dict:items:";
    private static final long CACHE_TTL_HOURS = 24;

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 字典类型管理 ====================

    /** 查询所有字典类型 */
    public List<DictType> listTypes() {
        return dictTypeMapper.selectList(
                new LambdaQueryWrapper<DictType>()
                        .eq(DictType::getStatus, "ACTIVE")
                        .orderByAsc(DictType::getSortOrder));
    }

    /** 新增字典类型 */
    @Transactional(rollbackFor = Exception.class)
    public DictType createType(DictType type) {
        DictType exist =
                dictTypeMapper.selectOne(
                        new LambdaQueryWrapper<DictType>()
                                .eq(DictType::getDictCode, type.getDictCode()));
        if (exist != null) {
            throw new BizException("字典类型编码已存在: " + type.getDictCode());
        }
        type.setStatus("ACTIVE");
        dictTypeMapper.insert(type);
        log.info("新增字典类型: {}", type.getDictCode());
        return type;
    }

    /** 更新字典类型 */
    @Transactional(rollbackFor = Exception.class)
    public void updateType(DictType type) {
        dictTypeMapper.updateById(type);
        log.info("更新字典类型: {}", type.getDictCode());
    }

    /** 删除字典类型（同时删除字典项） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        DictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BizException("字典类型不存在");
        }
        // 删除字典项
        dictItemMapper.delete(
                new LambdaQueryWrapper<DictItem>().eq(DictItem::getDictCode, type.getDictCode()));
        // 删除字典类型
        dictTypeMapper.deleteById(id);
        // 清除缓存
        evictCache(type.getDictCode());
        log.info("删除字典类型: {}", type.getDictCode());
    }

    // ==================== 字典项管理 ====================

    /** 根据字典类型编码查询字典项列表（带缓存） */
    @SuppressWarnings("unchecked")
    public List<DictItem> listItems(String dictCode) {
        String cacheKey = CACHE_PREFIX + dictCode;
        // 尝试从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (List<DictItem>) cached;
        }
        // 从数据库查询
        List<DictItem> items =
                dictItemMapper.selectList(
                        new LambdaQueryWrapper<DictItem>()
                                .eq(DictItem::getDictCode, dictCode)
                                .eq(DictItem::getStatus, "ACTIVE")
                                .orderByAsc(DictItem::getSortOrder));
        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, items, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return items;
    }

    /** 根据字典类型编码和父级值查询子字典项（级联字典） */
    public List<DictItem> listItemsByParent(String dictCode, String parentValue) {
        return listItems(dictCode).stream()
                .filter(item -> parentValue.equals(item.getParentValue()))
                .collect(Collectors.toList());
    }

    /** 新增字典项 */
    @Transactional(rollbackFor = Exception.class)
    public DictItem createItem(DictItem item) {
        // 校验字典类型存在
        DictType type =
                dictTypeMapper.selectOne(
                        new LambdaQueryWrapper<DictType>()
                                .eq(DictType::getDictCode, item.getDictCode()));
        if (type == null) {
            throw new BizException("字典类型不存在: " + item.getDictCode());
        }
        // 校验同类型下itemValue唯一
        DictItem exist =
                dictItemMapper.selectOne(
                        new LambdaQueryWrapper<DictItem>()
                                .eq(DictItem::getDictCode, item.getDictCode())
                                .eq(DictItem::getItemValue, item.getItemValue()));
        if (exist != null) {
            throw new BizException("字典项值已存在: " + item.getItemValue());
        }
        item.setStatus("ACTIVE");
        dictItemMapper.insert(item);
        // 清除缓存
        evictCache(item.getDictCode());
        log.info("新增字典项: {}={}", item.getDictCode(), item.getItemValue());
        return item;
    }

    /** 更新字典项 */
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(DictItem item) {
        dictItemMapper.updateById(item);
        evictCache(item.getDictCode());
        log.info("更新字典项: {}={}", item.getDictCode(), item.getItemValue());
    }

    /** 删除字典项 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BizException("字典项不存在");
        }
        dictItemMapper.deleteById(id);
        evictCache(item.getDictCode());
        log.info("删除字典项: {}={}", item.getDictCode(), item.getItemValue());
    }

    // ==================== 字典翻译 ====================

    /**
     * 字典翻译：itemValue → itemLabel
     *
     * @param dictCode 字典类型编码
     * @param itemValue 字典项值
     * @return 字典项标签，未找到返回原值
     */
    public String translate(String dictCode, String itemValue) {
        if (itemValue == null || itemValue.isEmpty()) {
            return itemValue;
        }
        return listItems(dictCode).stream()
                .filter(item -> itemValue.equals(item.getItemValue()))
                .map(DictItem::getItemLabel)
                .findFirst()
                .orElse(itemValue);
    }

    /** 批量翻译：Map<itemValue, itemLabel> */
    public Map<String, String> translateMap(String dictCode) {
        return listItems(dictCode).stream()
                .collect(
                        Collectors.toMap(
                                DictItem::getItemValue, DictItem::getItemLabel, (k1, k2) -> k1));
    }

    /** 反向翻译：itemLabel → itemValue */
    public String reverseTranslate(String dictCode, String itemLabel) {
        if (itemLabel == null || itemLabel.isEmpty()) {
            return itemLabel;
        }
        return listItems(dictCode).stream()
                .filter(item -> itemLabel.equals(item.getItemLabel()))
                .map(DictItem::getItemValue)
                .findFirst()
                .orElse(itemLabel);
    }

    // ==================== 缓存管理 ====================

    /** 清除指定字典类型的缓存 */
    public void evictCache(String dictCode) {
        redisTemplate.delete(CACHE_PREFIX + dictCode);
        log.debug("清除字典缓存: {}", dictCode);
    }

    /** 清除所有字典缓存 */
    public void evictAllCache() {
        var keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除所有字典缓存: {}个", keys.size());
        }
    }

    /** 刷新所有字典缓存 */
    public void refreshAllCache() {
        evictAllCache();
        List<DictType> types = listTypes();
        types.forEach(type -> listItems(type.getDictCode()));
        log.info("刷新所有字典缓存: {}个类型", types.size());
    }
}
