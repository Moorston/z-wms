package com.xwms.core.liteflow.component.allocation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.AllocationContext;

import lombok.extern.slf4j.Slf4j;

/** 库位属性过滤组件 按库位属性过滤候选库位： - 库位类型（拣货区优先） - 库位状态（正常/非冻结） - 库区（同区优先，减少搬运） - 货主隔离（多货主场景） */
@Slf4j
@LiteflowComponent("locationFilter")
public class LocationFilterComponent extends NodeComponent {

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] 库位过滤开始, 候选数={}", context.getCandidateLocations().size());

        // TODO: 按库位属性过滤
        List<Map<String, Object>> filtered =
                context.getCandidateLocations().stream()
                        .filter(loc -> "PICKING".equals(loc.get("locationType"))) // 拣货区优先
                        .filter(loc -> "NORMAL".equals(loc.get("status"))) // 正常库位
                        .collect(Collectors.toList());

        context.setCandidateLocations(filtered);
        log.info("[库存分配] 库位过滤完成, 剩余={}", filtered.size());
    }
}
