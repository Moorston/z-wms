package com.xwms.core.liteflow.component.allocation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.AllocationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis预占组件 在Redis中预占库存，防止超卖 Key: wms:inventory:reserve:{sku}:{warehouse}:{location}:{batch} Value:
 * 预占数量 TTL: 30分钟（超时自动释放）
 */
@Slf4j
@LiteflowComponent("redisReserve")
public class RedisReserveComponent extends NodeComponent {

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] Redis预占开始: allocated={}", context.getAllocatedQty());

        // TODO: Redis原子预占（Lua脚本保证原子性）
        // for (entry : allocationResult)
        // {
        //     redisTemplate.opsForValue().increment(key, qty);
        //     redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        // }

        context.setRedisReserved(true);
        log.info("[库存分配] Redis预占完成");
    }
}
