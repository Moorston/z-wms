package com.xwms.core.plugin.industry.gsp.service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 医药GSP温湿度记录服务 GSP要求： 1. 冷库温度2-8℃，阴凉库≤20℃，常温库0-30℃ 2. 相对湿度35%-75% 3. 每30分钟自动记录一次 4. 超温超湿立即报警 5.
 * 运输过程全程温度记录
 */
@Slf4j
@Service
public class GspTemperatureService {

    /** 温湿度标准 */
    private static final Map<String, double[]> TEMP_RANGE =
            Map.of(
                    "COLD", new double[] {2.0, 8.0}, // 冷库
                    "COOL", new double[] {0.0, 20.0}, // 阴凉库
                    "NORMAL", new double[] {0.0, 30.0} // 常温库
                    );

    private static final double[] HUMIDITY_RANGE = {35.0, 75.0};

    /** 温湿度记录 */
    public void record(String warehouse, String area, double temperature, double humidity) {
        log.info(
                "GSP温湿度记录: warehouse={}, area={}, temp={}℃, humidity={}%, time={}",
                warehouse, area, temperature, humidity, LocalDateTime.now());
        // TODO: 持久化到wms_gsp_temp_record表
        checkAlert(warehouse, area, temperature, humidity);
    }

    /** 温湿度超标检查 */
    public void checkAlert(String warehouse, String area, double temperature, double humidity) {
        // 温度检查
        double[] range = TEMP_RANGE.getOrDefault(area, TEMP_RANGE.get("NORMAL"));
        if (temperature < range[0] || temperature > range[1]) {
            log.error(
                    "GSP温度超标报警: warehouse={}, area={}, temp={}℃, 标准={}-{}℃",
                    warehouse,
                    area,
                    temperature,
                    range[0],
                    range[1]);
            // TODO: 触发报警（短信/飞书/系统消息）
        }
        // 湿度检查
        if (humidity < HUMIDITY_RANGE[0] || humidity > HUMIDITY_RANGE[1]) {
            log.error(
                    "GSP湿度超标报警: warehouse={}, area={}, humidity={}%, 标准={}-{}%",
                    warehouse, area, humidity, HUMIDITY_RANGE[0], HUMIDITY_RANGE[1]);
        }
    }

    /** 运输温度记录（冷链药品） */
    public void recordTransport(String orderNo, double temperature, LocalDateTime time) {
        log.info("GSP运输温度记录: orderNo={}, temp={}℃, time={}", orderNo, temperature, time);
        if (temperature < 2.0 || temperature > 8.0) {
            log.error("冷链运输温度超标: orderNo={}, temp={}℃", orderNo, temperature);
            // TODO: 触发断链预警，药品可能需要质量复检
        }
    }

    /** 验证库区温度是否符合药品存储要求 */
    public boolean validateStorageArea(String drugStorageType, String areaType) {
        // 冷藏药品必须放冷库
        if ("COLD".equals(drugStorageType) && !"COLD".equals(areaType)) {
            return false;
        }
        // 阴凉药品不能放常温库
        if ("COOL".equals(drugStorageType) && "NORMAL".equals(areaType)) {
            return false;
        }
        return true;
    }
}
