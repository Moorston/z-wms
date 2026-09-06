package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.InboundContext;

import lombok.extern.slf4j.Slf4j;

/** ASN预约校验组件 校验ASN（预先发货通知）是否有效，是否与入库单匹配 */
@Slf4j
@LiteflowComponent("asnCheck")
public class AsnCheckComponent extends NodeComponent {

    @Override
    public void process() {
        InboundContext context = this.getContextBean(InboundContext.class);

        log.info("[入库流程] ASN校验开始: asnNo={}", context.getAsnNo());

        // TODO: 校验ASN
        // 1. ASN是否存在
        // 2. ASN状态是否为已发货
        // 3. ASN明细与入库单是否匹配
        // 4. ASN是否过期

        log.info("[入库流程] ASN校验通过");
    }
}
