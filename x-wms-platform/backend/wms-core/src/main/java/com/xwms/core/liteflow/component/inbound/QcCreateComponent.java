package com.xwms.core.liteflow.component.inbound;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.qc.dto.QcCreateRequest;
import com.xwms.core.qc.entity.QcOrder;
import com.xwms.core.qc.service.QcOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow质检组件 - 入库流程中触发质检 在入库收货完成后, 根据质检规则创建质检单 */
@Slf4j
@LiteflowComponent("qcCreate")
@RequiredArgsConstructor
public class QcCreateComponent extends NodeComponent {

    private final QcOrderService qcOrderService;

    @Override
    public void process() {
        QcCreateRequest request = this.getContextBean(QcCreateRequest.class);
        if (request == null) {
            log.info("无质检请求, 跳过质检");
            return;
        }

        log.info("创建质检单: SKU={}, 批量={}", request.getSku(), request.getLotQty());
        QcOrder qcOrder = qcOrderService.createQcOrder(request);

        if (qcOrder != null) {
            this.getContextBean(com.xwms.core.liteflow.context.InboundContext.class)
                    .getQcResults()
                    .put("qcOrderId", String.valueOf(qcOrder.getId()));
            log.info("质检单创建成功: {}", qcOrder.getQcNo());
        } else {
            log.info("商品免检, 跳过质检");
        }
    }

    @Override
    public boolean isAccess() {
        // 只有入库类型需要质检
        return true;
    }
}
