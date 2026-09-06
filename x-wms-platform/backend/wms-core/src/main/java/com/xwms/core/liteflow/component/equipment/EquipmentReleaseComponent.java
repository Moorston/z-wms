package com.xwms.core.liteflow.component.equipment;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.equipment.service.EquipmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow设备组件 - 作业完成后自动释放设备 入库/出库/移库等作业完成后, 自动释放分配的设备 */
@Slf4j
@LiteflowComponent("equipmentRelease")
@RequiredArgsConstructor
public class EquipmentReleaseComponent extends NodeComponent {

    private final EquipmentService equipmentService;

    @Override
    public void process() {
        Long equipmentId = this.getContextBean(Long.class);
        if (equipmentId == null) {
            log.info("无设备ID, 跳过释放");
            return;
        }
        try {
            equipmentService.releaseEquipment(equipmentId, null, null);
            log.info("设备{}自动释放", equipmentId);
        } catch (Exception e) {
            log.error("设备{}自动释放失败: {}", equipmentId, e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
