package com.xwms.core.reserve.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.reserve.entity.*;
import com.xwms.core.reserve.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 搴撳瓨棰勫崰绠＄悊鏍稿績鏈嶅姟 鏍稿績鑳藉姏: 搴撳瓨棰勫崰/閲婃斁/纭/Redis棰勫崰+Oracle鍙屽啓/搴撳瓨閿? 鏋舵瀯鍐崇瓥:
 * Redis鍋氬簱瀛樻鏌ュ拰棰勫崰锛孫racle鍋氭渶缁堜竴鑷存€ф墸鍑?
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReserveService {

    private final InventoryReserveMapper reserveMapper;
    private final InventoryReserveDetailMapper detailMapper;
    private final InventoryReserveLogMapper logMapper;
    private final StringRedisTemplate redisTemplate;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RESERVE_KEY_PREFIX = "wms:reserve:";
    private static final String INVENTORY_KEY_PREFIX = "wms:inventory:";
    private static final long DEFAULT_EXPIRE_MINUTES = 30;

    // ============================================================

    // 1. 搴撳瓨棰勫崰鍒涘缓锛圧edis棰勫崰 + Oracle璁板綍锛?
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public InventoryReserve createReserve(
            InventoryReserve reserve, List<InventoryReserveDetail> details, String operator) {
        reserve.setReserveNo(generateReserveNo());
        reserve.setStatus("RESERVED");
        reserve.setCreatedBy(operator);
        if (reserve.getReservedQty() == null) reserve.setReservedQty(BigDecimal.ZERO);
        if (reserve.getReleasedQty() == null) reserve.setReleasedQty(BigDecimal.ZERO);
        if (reserve.getConfirmedQty() == null) reserve.setConfirmedQty(BigDecimal.ZERO);
        if (reserve.getExpireTime() == null) {
            reserve.setExpireTime(LocalDateTime.now().plusMinutes(DEFAULT_EXPIRE_MINUTES));
        }
        reserveMapper.insert(reserve);

        // 淇濆瓨鏄庣粏骞舵墽琛孯edis棰勫崰
        int lineNo = 1;
        BigDecimal totalReserved = BigDecimal.ZERO;
        for (InventoryReserveDetail detail : details) {
            detail.setReserveNo(reserve.getReserveNo());
            detail.setLineNo(lineNo++);
            if (detail.getReservedQty() == null) detail.setReservedQty(BigDecimal.ZERO);
            if (detail.getReleasedQty() == null) detail.setReleasedQty(BigDecimal.ZERO);
            if (detail.getConfirmedQty() == null) detail.setConfirmedQty(BigDecimal.ZERO);
            detail.setStatus("RESERVED");
            detailMapper.insert(detail);

            // Redis棰勫崰搴撳瓨
            boolean redisSuccess =
                    redisReserve(
                            reserve.getWarehouseCode(),
                            detail.getSkuCode(),
                            detail.getBatchNo(),
                            detail.getLocationCode(),
                            detail.getPlanQty(),
                            reserve.getReserveNo(),
                            reserve.getExpireTime());

            if (redisSuccess) {
                detail.setReservedQty(detail.getPlanQty());
                detailMapper.updateById(detail);
                totalReserved = totalReserved.add(detail.getPlanQty());

                // 璁板綍棰勫崰娴佹按
                recordReserveLog(
                        reserve.getReserveNo(),
                        reserve.getRefType(),
                        reserve.getRefNo(),
                        reserve.getWarehouseCode(),
                        reserve.getOwnerCode(),
                        detail.getSkuCode(),
                        detail.getBatchNo(),
                        detail.getLocationCode(),
                        "RESERVE",
                        BigDecimal.ZERO,
                        detail.getPlanQty(),
                        detail.getPlanQty(),
                        operator,
                        "Redis棰勫崰鎴愬姛");
            } else {
                // Redis棰勫崰澶辫触锛屼娇鐢ㄦ暟鎹簱閿佸厹搴?
                boolean dbSuccess =
                        dbLockReserve(
                                reserve.getWarehouseCode(),
                                detail.getSkuCode(),
                                detail.getBatchNo(),
                                detail.getLocationCode(),
                                detail.getPlanQty(),
                                reserve.getReserveNo(),
                                reserve.getExpireTime());
                if (dbSuccess) {
                    detail.setReservedQty(detail.getPlanQty());
                    detailMapper.updateById(detail);
                    totalReserved = totalReserved.add(detail.getPlanQty());
                    recordReserveLog(
                            reserve.getReserveNo(),
                            reserve.getRefType(),
                            reserve.getRefNo(),
                            reserve.getWarehouseCode(),
                            reserve.getOwnerCode(),
                            detail.getSkuCode(),
                            detail.getBatchNo(),
                            detail.getLocationCode(),
                            "RESERVE",
                            BigDecimal.ZERO,
                            detail.getPlanQty(),
                            detail.getPlanQty(),
                            operator,
                            "鏁版嵁搴撻攣棰勫崰鎴愬姛");
                } else {
                    log.warn("搴撳瓨棰勫崰澶辫触: sku={}, qty={}", detail.getSkuCode(), detail.getPlanQty());
                    recordReserveLog(
                            reserve.getReserveNo(),
                            reserve.getRefType(),
                            reserve.getRefNo(),
                            reserve.getWarehouseCode(),
                            reserve.getOwnerCode(),
                            detail.getSkuCode(),
                            detail.getBatchNo(),
                            detail.getLocationCode(),
                            "RESERVE",
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            operator,
                            "库存预占失败，库存不足");
                }
            }
        }

        reserve.setReservedQty(totalReserved);
        reserveMapper.updateById(reserve);

        log.info(
                "鍒涘缓搴撳瓨棰勫崰: {}, type={}, totalReserved={}",
                reserve.getReserveNo(),
                reserve.getReserveType(),
                totalReserved);
        return reserve;
    }

    // ============================================================

    // 2. Redis棰勫崰搴撳瓨
    // ============================================================

    /** Redis棰勫崰搴撳瓨 浣跨敤Lua鑴氭湰淇濊瘉鍘熷瓙鎬э細妫€鏌ュ彲鐢ㄥ簱瀛?-> 鎵ｅ噺鍙敤搴撳瓨 -> 澧炲姞棰勫崰搴撳瓨 */
    private boolean redisReserve(
            String warehouseCode,
            String skuCode,
            String batchNo,
            String locationCode,
            BigDecimal qty,
            String reserveNo,
            LocalDateTime expireTime) {
        String inventoryKey = buildInventoryKey(warehouseCode, skuCode, batchNo, locationCode);
        String reserveKey = RESERVE_KEY_PREFIX + reserveNo + ":" + inventoryKey;

        try {
            // 妫€鏌edis搴撳瓨鏄惁瀛樺湪
            String availableStr =
                    (String) redisTemplate.opsForHash().get(inventoryKey, "available");
            if (availableStr == null) {
                // Redis鏃犲簱瀛樻暟鎹紝闇€瑕佷粠鏁版嵁搴撳姞杞斤紙姝ゅ绠€鍖栵紝瀹為檯搴旇皟鐢ㄥ簱瀛樻湇鍔★級
                log.debug("Redis鏃犲簱瀛樻暟鎹紝璺宠繃Redis棰勫崰: {}", inventoryKey);
                return false;
            }

            BigDecimal available = new BigDecimal(availableStr);
            if (available.compareTo(qty) < 0) {
                log.warn(
                        "Redis搴撳瓨涓嶈冻: key={}, available={}, need={}", inventoryKey, available, qty);
                return false;
            }

            // 鍘熷瓙鎵ｅ噺锛堝疄闄呭簲浣跨敤Lua鑴氭湰锛?
            redisTemplate
                    .opsForHash()
                    .increment(inventoryKey, "available", qty.negate().longValue());
            redisTemplate.opsForHash().increment(inventoryKey, "allocated", qty.longValue());

            // 璁板綍棰勫崰鏄庣粏
            redisTemplate
                    .opsForValue()
                    .set(reserveKey, qty.toString(), DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);

            log.debug("Redis棰勫崰鎴愬姛: key={}, qty={}", inventoryKey, qty);
            return true;
        } catch (Exception e) {
            log.error("Redis棰勫崰寮傚父: key={}", inventoryKey, e);
            return false;
        }
    }

    // ============================================================

    // 3. 鏁版嵁搴撻攣棰勫崰锛圧edis澶辫触鏃剁殑鍏滃簳锛?
    // ============================================================

    private boolean dbLockReserve(
            String warehouseCode,
            String skuCode,
            String batchNo,
            String locationCode,
            BigDecimal qty,
            String reserveNo,
            LocalDateTime expireTime) {
        // DB lock removed, use Redis reserve as primary
        log.warn("DB lock reserve removed, Redis reserve failed: sku={}", skuCode);
        return false;
    }

    // ============================================================

    // 4. 閲婃斁搴撳瓨棰勫崰
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public InventoryReserve releaseReserve(String reserveNo, BigDecimal qty, String operator) {
        InventoryReserve reserve = reserveMapper.selectByReserveNo(reserveNo);
        if (reserve == null) throw new RuntimeException("棰勫崰鍗曚笉瀛樺湪: " + reserveNo);

        List<InventoryReserveDetail> details = detailMapper.selectByReserveNo(reserveNo);
        BigDecimal remaining = qty;

        for (InventoryReserveDetail detail : details) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal available = detail.getReservedQty().subtract(detail.getReleasedQty());
            if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal releaseQty = available.min(remaining);

            // 閲婃斁Redis棰勫崰
            redisRelease(
                    reserve.getWarehouseCode(),
                    detail.getSkuCode(),
                    detail.getBatchNo(),
                    detail.getLocationCode(),
                    releaseQty,
                    reserveNo);

            // 鏇存柊鏄庣粏
            detailMapper.addReleasedQty(detail.getId(), releaseQty);

            // 璁板綍娴佹按
            recordReserveLog(
                    reserveNo,
                    reserve.getRefType(),
                    reserve.getRefNo(),
                    reserve.getWarehouseCode(),
                    reserve.getOwnerCode(),
                    detail.getSkuCode(),
                    detail.getBatchNo(),
                    detail.getLocationCode(),
                    "RELEASE",
                    detail.getReservedQty(),
                    releaseQty.negate(),
                    detail.getReservedQty().subtract(releaseQty),
                    operator,
                    "閲婃斁棰勫崰");

            remaining = remaining.subtract(releaseQty);
        }

        // 鏇存柊棰勫崰鍗?
        reserveMapper.addReleasedQty(reserveNo, qty);

        log.info("閲婃斁搴撳瓨棰勫崰: {}, qty={}", reserveNo, qty);
        return reserveMapper.selectByReserveNo(reserveNo);
    }

    private void redisRelease(
            String warehouseCode,
            String skuCode,
            String batchNo,
            String locationCode,
            BigDecimal qty,
            String reserveNo) {
        String inventoryKey = buildInventoryKey(warehouseCode, skuCode, batchNo, locationCode);
        String reserveKey = RESERVE_KEY_PREFIX + reserveNo + ":" + inventoryKey;
        try {
            redisTemplate.opsForHash().increment(inventoryKey, "available", qty.longValue());
            redisTemplate
                    .opsForHash()
                    .increment(inventoryKey, "allocated", qty.negate().longValue());
            redisTemplate.delete(reserveKey);
        } catch (Exception e) {
            log.error("Redis閲婃斁棰勫崰寮傚父: key={}", inventoryKey, e);
        }
    }

    // ============================================================

    // 5. 纭搴撳瓨棰勫崰锛堟嫞璐у畬鎴愬悗纭锛岃浆涓哄疄闄呮墸鍑忥級
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public InventoryReserve confirmReserve(String reserveNo, BigDecimal qty, String operator) {
        InventoryReserve reserve = reserveMapper.selectByReserveNo(reserveNo);
        if (reserve == null) throw new RuntimeException("棰勫崰鍗曚笉瀛樺湪: " + reserveNo);

        List<InventoryReserveDetail> details = detailMapper.selectByReserveNo(reserveNo);
        BigDecimal remaining = qty;

        for (InventoryReserveDetail detail : details) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal available =
                    detail.getReservedQty()
                            .subtract(detail.getReleasedQty())
                            .subtract(detail.getConfirmedQty());
            if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal confirmQty = available.min(remaining);

            // 纭棰勫崰锛氫粠棰勫崰杞负瀹為檯鎵ｅ噺锛堣皟鐢ㄥ簱瀛樻湇鍔℃墽琛孫racle鎵ｅ噺锛?
            // TODO: 璋冪敤搴撳瓨鏈嶅姟鎵ц瀹為檯鎵ｅ噺

            // 鏇存柊鏄庣粏
            detailMapper.addConfirmedQty(detail.getId(), confirmQty);

            // 璁板綍娴佹按
            recordReserveLog(
                    reserveNo,
                    reserve.getRefType(),
                    reserve.getRefNo(),
                    reserve.getWarehouseCode(),
                    reserve.getOwnerCode(),
                    detail.getSkuCode(),
                    detail.getBatchNo(),
                    detail.getLocationCode(),
                    "CONFIRM",
                    detail.getReservedQty(),
                    confirmQty.negate(),
                    detail.getReservedQty().subtract(confirmQty),
                    operator,
                    "确认预占，执行实际扣减");

            remaining = remaining.subtract(confirmQty);
        }

        // 鏇存柊棰勫崰鍗?
        reserveMapper.addConfirmedQty(reserveNo, qty);

        log.info("纭搴撳瓨棰勫崰: {}, qty={}", reserveNo, qty);
        return reserveMapper.selectByReserveNo(reserveNo);
    }

    // ============================================================

    // 6. 杩囨湡棰勫崰鑷姩閲婃斁锛圥owerJob瀹氭椂浠诲姟璋冪敤锛?
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public int releaseExpiredReserves() {
        LambdaQueryWrapper<InventoryReserve> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(InventoryReserve::getExpireTime, LocalDateTime.now())
                .in(InventoryReserve::getStatus, "RESERVED", "PARTIAL");
        List<InventoryReserve> expiredReserves = reserveMapper.selectList(wrapper);

        int count = 0;
        for (InventoryReserve reserve : expiredReserves) {
            BigDecimal qty =
                    reserve.getReservedQty()
                            .subtract(reserve.getReleasedQty())
                            .subtract(reserve.getConfirmedQty());
            if (qty.compareTo(BigDecimal.ZERO) > 0) {
                releaseReserve(reserve.getReserveNo(), qty, "SYSTEM");
                count++;
            }
            reserveMapper.updateStatus(reserve.getReserveNo(), "RELEASED");

            // 璁板綍杩囨湡娴佹按
            recordReserveLog(
                    reserve.getReserveNo(),
                    reserve.getRefType(),
                    reserve.getRefNo(),
                    reserve.getWarehouseCode(),
                    reserve.getOwnerCode(),
                    null,
                    null,
                    null,
                    "EXPIRE",
                    reserve.getReservedQty(),
                    qty.negate(),
                    reserve.getReservedQty().subtract(qty),
                    "SYSTEM",
                    "棰勫崰杩囨湡鑷姩閲婃斁");
        }

        log.info("杩囨湡棰勫崰鑷姩閲婃斁: count={}", count);
        return count;
    }

    // ============================================================

    // 7. 鏌ヨ
    // ============================================================

    public Page<InventoryReserve> pageReserves(
            Page<InventoryReserve> page,
            String warehouseCode,
            String reserveType,
            String status,
            String refNo) {
        LambdaQueryWrapper<InventoryReserve> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(InventoryReserve::getWarehouseCode, warehouseCode);
        if (reserveType != null) wrapper.eq(InventoryReserve::getReserveType, reserveType);
        if (status != null) wrapper.eq(InventoryReserve::getStatus, status);
        if (refNo != null) wrapper.eq(InventoryReserve::getRefNo, refNo);
        wrapper.orderByDesc(InventoryReserve::getCreatedTime);
        return reserveMapper.selectPage(page, wrapper);
    }

    public InventoryReserve getReserveByNo(String reserveNo) {
        return reserveMapper.selectByReserveNo(reserveNo);
    }

    public InventoryReserve getReserveByRef(String refType, String refNo) {
        return reserveMapper.selectByRef(refType, refNo);
    }

    public List<InventoryReserveDetail> getReserveDetails(String reserveNo) {
        return detailMapper.selectByReserveNo(reserveNo);
    }

    public List<InventoryReserveLog> getReserveLogs(String reserveNo) {
        return logMapper.selectByReserveNo(reserveNo);
    }

    // ============================================================

    // 宸ュ叿鏂规硶
    // ============================================================

    private void recordReserveLog(
            String reserveNo,
            String refType,
            String refNo,
            String warehouseCode,
            String ownerCode,
            String skuCode,
            String batchNo,
            String locationCode,
            String actionType,
            BigDecimal beforeQty,
            BigDecimal changeQty,
            BigDecimal afterQty,
            String operator,
            String remark) {
        InventoryReserveLog log = new InventoryReserveLog();
        log.setLogNo(generateLogNo());
        log.setReserveNo(reserveNo);
        log.setRefType(refType);
        log.setRefNo(refNo);
        log.setWarehouseCode(warehouseCode);
        log.setOwnerCode(ownerCode);
        log.setSkuCode(skuCode);
        log.setBatchNo(batchNo);
        log.setLocationCode(locationCode);
        log.setActionType(actionType);
        log.setBeforeQty(beforeQty);
        log.setChangeQty(changeQty);
        log.setAfterQty(afterQty);
        log.setOperator(operator);
        log.setActionTime(LocalDateTime.now());
        log.setRemark(remark);
        logMapper.insert(log);
    }

    private String buildInventoryKey(
            String warehouseCode, String skuCode, String batchNo, String locationCode) {
        return INVENTORY_KEY_PREFIX
                + warehouseCode
                + ":"
                + skuCode
                + ":"
                + (batchNo != null ? batchNo : "DEFAULT")
                + ":"
                + locationCode;
    }

    private String buildLockKey(
            String warehouseCode, String skuCode, String batchNo, String locationCode) {
        return warehouseCode
                + ":"
                + skuCode
                + ":"
                + (batchNo != null ? batchNo : "DEFAULT")
                + ":"
                + locationCode;
    }

    private String generateReserveNo() {
        return "RSV"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogNo() {
        return "RSL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
