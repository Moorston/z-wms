package com.xwms.core.liteflow;

import org.springframework.stereotype.Service;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;

import com.xwms.core.liteflow.context.WaveExecuteContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow流程执行服务 封装FlowExecutor，提供业务友好的API */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiteFlowService {

    private final FlowExecutor flowExecutor;

    /**
     * 执行波次流程
     *
     * @param context 波次执行上下文
     * @return 执行结果
     */
    public LiteflowResponse executeWave(WaveExecuteContext context) {
        log.info("[LiteFlow] 开始执行波次流程: waveNo={}", context.getWaveNo());
        long start = System.currentTimeMillis();

        LiteflowResponse response = flowExecutor.execute2Resp("waveExecute", null, context);

        long cost = System.currentTimeMillis() - start;
        if (response.isSuccess()) {
            log.info("[LiteFlow] 波次流程执行成功: waveNo={}, cost={}ms", context.getWaveNo(), cost);
        } else {
            log.error(
                    "[LiteFlow] 波次流程执行失败: waveNo={}, cost={}ms, error={}",
                    context.getWaveNo(),
                    cost,
                    response.getMessage());
        }
        return response;
    }

    /**
     * 执行指定流程
     *
     * @param chainId 流程ID
     * @param context 上下文
     * @return 执行结果
     */
    public LiteflowResponse execute(String chainId, Object context) {
        log.info("[LiteFlow] 开始执行流程: chainId={}", chainId);
        long start = System.currentTimeMillis();

        LiteflowResponse response = flowExecutor.execute2Resp(chainId, null, context);

        long cost = System.currentTimeMillis() - start;
        log.info(
                "[LiteFlow] 流程执行完成: chainId={}, success={}, cost={}ms",
                chainId,
                response.isSuccess(),
                cost);
        return response;
    }
}
