package com.xwms.core.pack.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.pack.entity.*;
import com.xwms.core.pack.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 打包管理核心服务 核心能力: 打包单创建/复核/打包/包裹管理/称重 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackService {

    private final PackMapper packMapper;
    private final PackDetailMapper detailMapper;
    private final PackageMapper packageMapper;
    private final CheckRecordMapper checkRecordMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 打包单创建
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Pack createPack(Pack pack, List<PackDetail> details, String operator) {
        if (pack.getPackNo() == null) {
            pack.setPackNo(generatePackNo());
        }
        if (pack.getStatus() == null) pack.setStatus("CREATED");
        if (pack.getTotalQty() == null) pack.setTotalQty(BigDecimal.ZERO);
        if (pack.getCheckedQty() == null) pack.setCheckedQty(BigDecimal.ZERO);
        if (pack.getPackedQty() == null) pack.setPackedQty(BigDecimal.ZERO);
        if (pack.getPackageCount() == null) pack.setPackageCount(0);
        if (pack.getTotalWeight() == null) pack.setTotalWeight(BigDecimal.ZERO);
        if (pack.getTotalVolume() == null) pack.setTotalVolume(BigDecimal.ZERO);
        pack.setCreatedBy(operator);
        packMapper.insert(pack);

        // 保存明细
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                PackDetail detail = details.get(i);
                detail.setPackNo(pack.getPackNo());
                detail.setLineNo(i + 1);
                if (detail.getExpectedQty() == null) detail.setExpectedQty(BigDecimal.ZERO);
                if (detail.getCheckedQty() == null) detail.setCheckedQty(BigDecimal.ZERO);
                if (detail.getPackedQty() == null) detail.setPackedQty(BigDecimal.ZERO);
                if (detail.getDifferenceQty() == null) detail.setDifferenceQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus("PENDING");
                detailMapper.insert(detail);
            }
            pack.setTotalQty(
                    details.stream()
                            .map(PackDetail::getExpectedQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            packMapper.updateById(pack);
        }

        log.info(
                "创建打包单: {}={}, 明细{}行",
                pack.getPackNo(),
                pack.getOutboundNo(),
                details != null ? details.size() : 0);
        return pack;
    }

    // ============================================================

    // 2. 复核
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public CheckRecord check(
            String packNo,
            String skuCode,
            String batchNo,
            BigDecimal expectedQty,
            BigDecimal actualQty,
            String checker) {
        Pack pack = packMapper.selectByPackNo(packNo);
        if (pack == null) throw new RuntimeException("打包单不存在: " + packNo);

        if ("CREATED".equals(pack.getStatus())) {
            pack.setStatus("CHECKING");
            pack.setChecker(checker);
            pack.setCheckTime(LocalDateTime.now());
            packMapper.updateById(pack);
        }

        // 计算差异
        BigDecimal differenceQty = actualQty.subtract(expectedQty);
        String checkResult;
        if (differenceQty.compareTo(BigDecimal.ZERO) == 0) {
            checkResult = "PASS";
        } else if (differenceQty.compareTo(BigDecimal.ZERO) < 0) {
            checkResult = "DIFFERENCE";
        } else {
            checkResult = "FAIL";
        }

        // 保存复核记录
        CheckRecord record = new CheckRecord();
        record.setRecordNo(generateRecordNo());
        record.setPackNo(packNo);
        record.setOutboundNo(pack.getOutboundNo());
        record.setSkuCode(skuCode);
        record.setBatchNo(batchNo);
        record.setExpectedQty(expectedQty);
        record.setActualQty(actualQty);
        record.setDifferenceQty(differenceQty);
        record.setCheckResult(checkResult);
        record.setChecker(checker);
        record.setCheckTime(LocalDateTime.now());
        checkRecordMapper.insert(record);

        // 更新明细复核数量
        List<PackDetail> details = detailMapper.selectByPackNo(packNo);
        for (PackDetail detail : details) {
            if (skuCode.equals(detail.getSkuCode())) {
                detail.setCheckedQty(detail.getCheckedQty().add(actualQty));
                detail.setDifferenceQty(detail.getDifferenceQty().add(differenceQty));
                detail.setStatus("CHECKED");
                detailMapper.updateById(detail);
                break;
            }
        }

        // 更新打包单复核数量
        pack.setCheckedQty(pack.getCheckedQty().add(actualQty));
        packMapper.updateById(pack);

        // 检查复核是否完成
        checkCheckComplete(packNo);

        log.info(
                "复核: pack={}, sku={}, expected={}, actual={}, result={}",
                packNo,
                skuCode,
                expectedQty,
                actualQty,
                checkResult);
        return record;
    }

    /** 检查复核是否完成 */
    private void checkCheckComplete(String packNo) {
        Pack pack = packMapper.selectByPackNo(packNo);
        List<PackDetail> details = detailMapper.selectByPackNo(packNo);
        boolean allChecked = details.stream().allMatch(d -> "CHECKED".equals(d.getStatus()));
        if (allChecked) {
            pack.setStatus("CHECKED");
            packMapper.updateById(pack);
            log.info("打包单复核完成: {}", packNo);
        }
    }

    // ============================================================

    // 3. 打包
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PackageEntity pack(
            String packNo,
            String packageType,
            String packageSize,
            BigDecimal weight,
            BigDecimal volume,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            Integer itemCount,
            Integer skuCount,
            String packer) {
        Pack pack = packMapper.selectByPackNo(packNo);
        if (pack == null) throw new RuntimeException("打包单不存在: " + packNo);

        if ("CHECKED".equals(pack.getStatus())) {
            pack.setStatus("PACKING");
            pack.setPacker(packer);
            pack.setPackTime(LocalDateTime.now());
            packMapper.updateById(pack);
        }

        // 创建包裹
        PackageEntity pkg = new PackageEntity();
        pkg.setPackageNo(generatePackageNo());
        pkg.setPackNo(packNo);
        pkg.setOutboundNo(pack.getOutboundNo());
        pkg.setPackageType(packageType);
        pkg.setPackageSize(packageSize);
        pkg.setWeight(weight);
        pkg.setVolume(volume);
        pkg.setLength(length);
        pkg.setWidth(width);
        pkg.setHeight(height);
        pkg.setItemCount(itemCount);
        pkg.setSkuCount(skuCount);
        pkg.setStatus("PACKED");
        pkg.setPacker(packer);
        pkg.setPackTime(LocalDateTime.now());
        packageMapper.insert(pkg);

        // 更新打包单
        pack.setPackageCount(pack.getPackageCount() + 1);
        pack.setTotalWeight(pack.getTotalWeight().add(weight != null ? weight : BigDecimal.ZERO));
        pack.setTotalVolume(pack.getTotalVolume().add(volume != null ? volume : BigDecimal.ZERO));
        pack.setPackedQty(
                pack.getPackedQty().add(BigDecimal.valueOf(itemCount != null ? itemCount : 0)));
        packMapper.updateById(pack);

        // 检查打包是否完成
        checkPackComplete(packNo);

        log.info(
                "打包: pack={}, package={}, type={}, weight={}",
                packNo,
                pkg.getPackageNo(),
                packageType,
                weight);
        return pkg;
    }

    /** 检查打包是否完成 */
    private void checkPackComplete(String packNo) {
        Pack pack = packMapper.selectByPackNo(packNo);
        if (pack.getPackedQty().compareTo(pack.getCheckedQty()) >= 0) {
            pack.setStatus("PACKED");
            packMapper.updateById(pack);
            log.info("打包单打包完成: {}", packNo);
        }
    }

    // ============================================================

    // 4. 包裹称重
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PackageEntity weighPackage(String packageNo, BigDecimal weight, BigDecimal volume) {
        PackageEntity pkg = packageMapper.selectByPackageNo(packageNo);
        if (pkg == null) throw new RuntimeException("包裹不存在: " + packageNo);

        pkg.setWeight(weight);
        pkg.setVolume(volume);
        pkg.setStatus("WEIGHED");
        pkg.setWeighTime(LocalDateTime.now());
        packageMapper.updateById(pkg);

        log.info("包裹称重: package={}, weight={}, volume={}", packageNo, weight, volume);
        return pkg;
    }

    // ============================================================

    // 5. 包裹贴标
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PackageEntity labelPackage(String packageNo, String trackingNo, String carrier) {
        PackageEntity pkg = packageMapper.selectByPackageNo(packageNo);
        if (pkg == null) throw new RuntimeException("包裹不存在: " + packageNo);

        pkg.setTrackingNo(trackingNo);
        pkg.setCarrier(carrier);
        pkg.setStatus("LABELED");
        pkg.setLabelTime(LocalDateTime.now());
        packageMapper.updateById(pkg);

        log.info("包裹贴标: package={}, tracking={}, carrier={}", packageNo, trackingNo, carrier);
        return pkg;
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public Page<Pack> pagePacks(
            Page<Pack> page,
            String outboundNo,
            String waveNo,
            String status,
            String warehouseCode) {
        LambdaQueryWrapper<Pack> wrapper = new LambdaQueryWrapper<>();
        if (outboundNo != null) wrapper.eq(Pack::getOutboundNo, outboundNo);
        if (waveNo != null) wrapper.eq(Pack::getWaveNo, waveNo);
        if (status != null) wrapper.eq(Pack::getStatus, status);
        if (warehouseCode != null) wrapper.eq(Pack::getWarehouseCode, warehouseCode);
        wrapper.orderByDesc(Pack::getCreatedTime);
        return packMapper.selectPage(page, wrapper);
    }

    public Pack getPackByNo(String packNo) {
        return packMapper.selectByPackNo(packNo);
    }

    public List<PackDetail> getPackDetails(String packNo) {
        return detailMapper.selectByPackNo(packNo);
    }

    public List<PackageEntity> getPackages(String packNo) {
        return packageMapper.selectByPackNo(packNo);
    }

    public List<CheckRecord> getCheckRecords(String packNo) {
        return checkRecordMapper.selectByPackNo(packNo);
    }

    public PackageEntity getPackageByNo(String packageNo) {
        return packageMapper.selectByPackageNo(packageNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generatePackNo() {
        return "PK"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generatePackageNo() {
        return "PKG"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRecordNo() {
        return "CHK"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
