package com.xwms.core.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TMS（运输管理系统）集成服务 负责WMS与TMS/物流系统之间的通信，包括： 1. 运输计划（发货计划/承运商分配/路线规划） 2. 发运通知（出库完成后通知TMS发车） 3.
 * 物流跟踪（运单状态/车辆位置/签收状态） 4. 回单管理（电子回单/纸质回单/回单确认） 5. 运费结算（运费计算/费用对账）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TmsIntegrationService {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 运输计划
    // ============================================================

    /**
     * 创建运输计划 出库单审核通过后，创建运输计划并推送给TMS
     *
     * @param outboundNo 出库单号
     * @param carrierCode 承运商编码
     * @param vehicleType 车型
     * @param expectedShipTime 预计发货时间
     * @return TMS运输计划编号
     */
    public String createTransportPlan(
            String outboundNo,
            String carrierCode,
            String vehicleType,
            LocalDateTime expectedShipTime) {
        log.info(
                "创建TMS运输计划: outboundNo={}, carrier={}, vehicle={}",
                outboundNo,
                carrierCode,
                vehicleType);

        try {
            // 构建运输计划
            TmsTransportPlan plan = new TmsTransportPlan();
            plan.setPlanNo(generatePlanNo());
            plan.setOutboundNo(outboundNo);
            plan.setCarrierCode(carrierCode);
            plan.setVehicleType(vehicleType);
            plan.setExpectedShipTime(expectedShipTime);
            plan.setStatus("PENDING");
            plan.setCreateTime(LocalDateTime.now());

            // TODO: 实际项目中通过Feign调用TMS接口或wms-integration的TMS适配器
            log.info("TMS运输计划创建成功: planNo={}", plan.getPlanNo());

            // 记录计划映射关系
            recordPlanMapping(outboundNo, plan.getPlanNo());

            return plan.getPlanNo();
        } catch (Exception e) {
            log.error("TMS运输计划创建失败: outboundNo={}, error={}", outboundNo, e.getMessage());
            throw new RuntimeException("TMS运输计划创建失败: " + e.getMessage(), e);
        }
    }

    /** 取消运输计划 */
    public boolean cancelTransportPlan(String planNo, String reason) {
        log.info("取消TMS运输计划: planNo={}, reason={}", planNo, reason);

        try {
            // TODO: 实际项目中调用TMS取消接口
            log.info("TMS运输计划取消成功: planNo={}", planNo);
            return true;
        } catch (Exception e) {
            log.error("TMS运输计划取消失败: planNo={}, error={}", planNo, e.getMessage());
            return false;
        }
    }

    /** 分配承运商 */
    public boolean assignCarrier(String planNo, String carrierCode, String carrierName) {
        log.info("分配承运商: planNo={}, carrier={}", planNo, carrierCode);

        try {
            // TODO: 实际项目中调用TMS承运商分配接口
            log.info("承运商分配成功: planNo={}, carrier={}", planNo, carrierName);
            return true;
        } catch (Exception e) {
            log.error("承运商分配失败: planNo={}, error={}", planNo, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 2. 发运通知
    // ============================================================

    /**
     * 发运通知 出库单发货完成后，通知TMS发车
     *
     * @param outboundNo 出库单号
     * @param planNo 运输计划号
     * @param vehicleNo 车牌号
     * @param driverName 司机姓名
     * @param driverPhone 司机电话
     * @param actualShipTime 实际发货时间
     * @return 是否成功
     */
    public boolean notifyShipment(
            String outboundNo,
            String planNo,
            String vehicleNo,
            String driverName,
            String driverPhone,
            LocalDateTime actualShipTime) {
        log.info(
                "TMS发运通知: outboundNo={}, planNo={}, vehicle={}, driver={}",
                outboundNo,
                planNo,
                vehicleNo,
                driverName);

        try {
            // 构建发运通知
            TmsShipmentNotification notification = new TmsShipmentNotification();
            notification.setOutboundNo(outboundNo);
            notification.setPlanNo(planNo);
            notification.setVehicleNo(vehicleNo);
            notification.setDriverName(driverName);
            notification.setDriverPhone(driverPhone);
            notification.setActualShipTime(actualShipTime);
            notification.setNotifyTime(LocalDateTime.now());

            // TODO: 实际项目中调用TMS发运通知接口
            log.info("TMS发运通知成功: outboundNo={}", outboundNo);

            // 更新WMS出库单发运状态
            updateOutboundShipStatus(outboundNo, "SHIPPED", actualShipTime);

            return true;
        } catch (Exception e) {
            log.error("TMS发运通知失败: outboundNo={}, error={}", outboundNo, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 3. 物流跟踪
    // ============================================================

    /**
     * 查询运单状态
     *
     * @param waybillNo 运单号
     * @return 运单状态
     */
    public TmsWaybillStatus queryWaybillStatus(String waybillNo) {
        log.info("查询TMS运单状态: waybillNo={}", waybillNo);

        // TODO: 实际项目中调用TMS运单查询接口
        TmsWaybillStatus status = mockQueryWaybillStatus(waybillNo);

        // 更新本地运单状态
        updateLocalWaybillStatus(status);

        return status;
    }

    /** 批量查询运单状态 */
    public List<TmsWaybillStatus> batchQueryWaybillStatus(List<String> waybillNos) {
        log.info("批量查询TMS运单状态: count={}", waybillNos.size());

        List<TmsWaybillStatus> statusList = new ArrayList<>();
        for (String waybillNo : waybillNos) {
            try {
                TmsWaybillStatus status = queryWaybillStatus(waybillNo);
                statusList.add(status);
            } catch (Exception e) {
                log.error("查询运单状态失败: waybillNo={}, error={}", waybillNo, e.getMessage());
            }
        }

        return statusList;
    }

    /** 接收TMS运单状态回传 TMS回调接口，更新运单状态 */
    public boolean receiveWaybillStatus(
            String waybillNo,
            String status,
            String location,
            LocalDateTime updateTime,
            String remark) {
        log.info("接收TMS运单状态回传: waybillNo={}, status={}, location={}", waybillNo, status, location);

        try {
            // 1. 更新本地运单状态
            TmsWaybillStatus waybillStatus = new TmsWaybillStatus();
            waybillStatus.setWaybillNo(waybillNo);
            waybillStatus.setStatus(status);
            waybillStatus.setCurrentLocation(location);
            waybillStatus.setUpdateTime(updateTime);
            waybillStatus.setRemark(remark);
            updateLocalWaybillStatus(waybillStatus);

            // 2. 如果已签收，触发回单流程
            if ("SIGNED".equals(status)) {
                handleWaybillSigned(waybillNo, updateTime);
            }

            // 3. 如果异常，触发异常处理
            if ("EXCEPTION".equals(status) || "DELAYED".equals(status)) {
                handleWaybillException(waybillNo, status, remark);
            }

            log.info("TMS运单状态回传处理成功: waybillNo={}, status={}", waybillNo, status);
            return true;
        } catch (Exception e) {
            log.error("TMS运单状态回传处理失败: waybillNo={}, error={}", waybillNo, e.getMessage());
            return false;
        }
    }

    /** 查询车辆位置 */
    public TmsVehicleLocation queryVehicleLocation(String vehicleNo) {
        log.info("查询TMS车辆位置: vehicleNo={}", vehicleNo);

        // TODO: 实际项目中调用TMS车辆位置查询接口
        return mockQueryVehicleLocation(vehicleNo);
    }

    // ============================================================

    // 4. 回单管理
    // ============================================================

    /**
     * 获取电子回单
     *
     * @param waybillNo 运单号
     * @return 电子回单URL
     */
    public String getElectronicProofOfDelivery(String waybillNo) {
        log.info("获取TMS电子回单: waybillNo={}", waybillNo);

        try {
            // TODO: 实际项目中调用TMS电子回单接口
            String podUrl = "https://tms.example.com/pod/" + waybillNo + ".pdf";
            log.info("获取电子回单成功: waybillNo={}, url={}", waybillNo, podUrl);
            return podUrl;
        } catch (Exception e) {
            log.error("获取电子回单失败: waybillNo={}, error={}", waybillNo, e.getMessage());
            return null;
        }
    }

    /** 确认回单 */
    public boolean confirmProofOfDelivery(
            String waybillNo, String confirmBy, String confirmRemark) {
        log.info("确认TMS回单: waybillNo={}, confirmBy={}", waybillNo, confirmBy);

        try {
            // TODO: 实际项目中调用TMS回单确认接口
            log.info("TMS回单确认成功: waybillNo={}", waybillNo);

            // 更新WMS出库单回单状态
            updateOutboundPodStatus(waybillNo, "CONFIRMED", confirmBy, confirmRemark);

            return true;
        } catch (Exception e) {
            log.error("TMS回单确认失败: waybillNo={}, error={}", waybillNo, e.getMessage());
            return false;
        }
    }

    /** 回单异常处理 */
    public boolean handlePodException(
            String waybillNo, String exceptionType, String exceptionDesc) {
        log.warn(
                "处理TMS回单异常: waybillNo={}, type={}, desc={}",
                waybillNo,
                exceptionType,
                exceptionDesc);

        try {
            // TODO: 实际项目中记录回单异常并通知相关人员
            log.info("TMS回单异常处理完成: waybillNo={}", waybillNo);
            return true;
        } catch (Exception e) {
            log.error("TMS回单异常处理失败: waybillNo={}, error={}", waybillNo, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 5. 运费结算
    // ============================================================

    /** 计算运费 */
    public BigDecimal calculateFreight(
            String waybillNo,
            BigDecimal weight,
            BigDecimal volume,
            String carrierCode,
            String routeCode) {
        log.info("计算TMS运费: waybillNo={}, weight={}, volume={}", waybillNo, weight, volume);

        try {
            // TODO: 实际项目中调用TMS运费计算接口
            BigDecimal freight =
                    weight.multiply(new BigDecimal("2.5"))
                            .add(volume.multiply(new BigDecimal("50")));
            log.info("TMS运费计算成功: waybillNo={}, freight={}", waybillNo, freight);
            return freight;
        } catch (Exception e) {
            log.error("TMS运费计算失败: waybillNo={}, error={}", waybillNo, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /** 运费对账 */
    public boolean reconcileFreight(String carrierCode, String billMonth, BigDecimal totalAmount) {
        log.info("TMS运费对账: carrier={}, month={}, amount={}", carrierCode, billMonth, totalAmount);

        try {
            // TODO: 实际项目中调用TMS运费对账接口
            log.info("TMS运费对账成功: carrier={}, month={}", carrierCode, billMonth);
            return true;
        } catch (Exception e) {
            log.error("TMS运费对账失败: carrier={}, error={}", carrierCode, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 6. 辅助方法
    // ============================================================

    private void recordPlanMapping(String outboundNo, String planNo) {
        // TODO: 实际项目中保存计划映射关系
        log.debug("记录运输计划映射: outboundNo={}, planNo={}", outboundNo, planNo);
    }

    private void updateOutboundShipStatus(
            String outboundNo, String status, LocalDateTime shipTime) {
        // TODO: 实际项目中更新WMS出库单发运状态
        log.debug("更新出库单发运状态: outboundNo={}, status={}", outboundNo, status);
    }

    private void updateLocalWaybillStatus(TmsWaybillStatus status) {
        // TODO: 实际项目中更新本地运单状态表
        log.debug("更新本地运单状态: waybillNo={}, status={}", status.getWaybillNo(), status.getStatus());
    }

    private void handleWaybillSigned(String waybillNo, LocalDateTime signTime) {
        // TODO: 实际项目中触发回单流程
        log.info("处理运单签收: waybillNo={}, signTime={}", waybillNo, signTime);
    }

    private void handleWaybillException(String waybillNo, String status, String remark) {
        // TODO: 实际项目中触发异常处理
        log.warn("处理运单异常: waybillNo={}, status={}, remark={}", waybillNo, status, remark);
    }

    private void updateOutboundPodStatus(
            String waybillNo, String status, String confirmBy, String confirmRemark) {
        // TODO: 实际项目中更新WMS出库单回单状态
        log.debug("更新出库单回单状态: waybillNo={}, status={}", waybillNo, status);
    }

    private String generatePlanNo() {
        return "TPL"
                + NO_FMT.format(LocalDateTime.now())
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    // ============================================================

    // 7. 模拟数据
    // ============================================================

    private TmsWaybillStatus mockQueryWaybillStatus(String waybillNo) {
        TmsWaybillStatus status = new TmsWaybillStatus();
        status.setWaybillNo(waybillNo);
        status.setStatus("IN_TRANSIT");
        status.setCurrentLocation("广州市白云区");
        status.setEstimatedArrivalTime(LocalDateTime.now().plusHours(24));
        status.setUpdateTime(LocalDateTime.now());
        return status;
    }

    private TmsVehicleLocation mockQueryVehicleLocation(String vehicleNo) {
        TmsVehicleLocation location = new TmsVehicleLocation();
        location.setVehicleNo(vehicleNo);
        location.setLatitude(new BigDecimal("23.1291"));
        location.setLongitude(new BigDecimal("113.2644"));
        location.setCurrentAddress("广州市白云区");
        location.setSpeed(new BigDecimal("60"));
        location.setUpdateTime(LocalDateTime.now());
        return location;
    }

    // ============================================================

    // 8. 数据模型
    // ============================================================

    @Data
    public static class TmsTransportPlan {
        private String planNo;
        private String outboundNo;
        private String carrierCode;
        private String carrierName;
        private String vehicleType;
        private String vehicleNo;
        private String driverName;
        private String driverPhone;
        private LocalDateTime expectedShipTime;
        private LocalDateTime actualShipTime;
        private String status; // PENDING待分配/ASSIGNED已分配/SHIPPED已发货/DELIVERED已送达/CANCELLED已取消
        private LocalDateTime createTime;
    }

    @Data
    public static class TmsShipmentNotification {
        private String outboundNo;
        private String planNo;
        private String vehicleNo;
        private String driverName;
        private String driverPhone;
        private LocalDateTime actualShipTime;
        private LocalDateTime notifyTime;
    }

    @Data
    public static class TmsWaybillStatus {
        private String waybillNo;
        private String outboundNo;
        private String carrierCode;
        private String
                status; // PENDING待发货/SHIPPED已发货/IN_TRANSIT运输中/DELIVERED已送达/SIGNED已签收/EXCEPTION异常/DELAYED延误
        private String currentLocation;
        private String nextLocation;
        private LocalDateTime estimatedArrivalTime;
        private LocalDateTime actualArrivalTime;
        private LocalDateTime signTime;
        private String signer;
        private LocalDateTime updateTime;
        private String remark;
    }

    @Data
    public static class TmsVehicleLocation {
        private String vehicleNo;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String currentAddress;
        private BigDecimal speed;
        private BigDecimal mileage;
        private LocalDateTime updateTime;
    }
}
