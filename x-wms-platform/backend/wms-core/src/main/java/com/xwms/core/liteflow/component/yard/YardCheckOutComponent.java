package com.xwms.core.liteflow.component.yard;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.yard.service.YardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow预约月台组件 - 出入库完成后自动签退离场 入库完成/出库发运后, 自动完成预约签退, 释放月台 */
@Slf4j
@LiteflowComponent("yardCheckOut")
@RequiredArgsConstructor
public class YardCheckOutComponent extends NodeComponent {

    private final YardService yardService;

    @Override
    public void process() {
        Long appointId = this.getContextBean(Long.class);
        if (appointId == null) {
            log.info("无预约单ID, 跳过");
            return;
        }
        try {
            yardService.checkOut(appointId);
            log.info("预约单{}自动签退离场", appointId);
        } catch (Exception e) {
            log.error("预约单{}自动签退失败: {}", appointId, e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
