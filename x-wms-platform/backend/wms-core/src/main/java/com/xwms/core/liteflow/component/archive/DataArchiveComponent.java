package com.xwms.core.liteflow.component.archive;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.archive.service.ArchiveService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow数据归档组件 - 业务流程中触发数据归档 订单完成后触发历史数据归档 */
@Slf4j
@LiteflowComponent("dataArchive")
@RequiredArgsConstructor
public class DataArchiveComponent extends NodeComponent {

    private final ArchiveService archiveService;

    @Override
    public void process() {
        Long ruleId = this.getContextBean(Long.class);
        if (ruleId == null) {
            log.info("无归档规则ID, 跳过");
            return;
        }
        try {
            archiveService.executeArchive(ruleId, "LITEFLOW");
            log.info("流程触发数据归档: 规则ID={}", ruleId);
        } catch (Exception e) {
            log.error("流程数据归档失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
