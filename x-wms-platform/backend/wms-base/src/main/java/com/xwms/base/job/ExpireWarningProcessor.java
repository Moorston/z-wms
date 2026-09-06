package com.xwms.base.job;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.base.master.entity.Owner;
import com.xwms.base.master.mapper.OwnerMapper;
import com.xwms.common.job.AbstractPowerJobProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;

/**
 * 批次效期预警定时任务
 *
 * <p>业务场景： 1. 每天扫描近效期批次（30天内到期） 2. 按货主分组生成预警记录 3. 近效期7天内的批次自动冻结 4. 发送预警通知（邮件/钉钉/企微）
 *
 * <p>执行频率：每小时 Cron: 0 0 * * * ?
 *
 * <p>任务参数（可选）： - {"ownerCode":"OWNER001","warningDays":30,"freezeDays":7}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpireWarningProcessor extends AbstractPowerJobProcessor {

    private final OwnerMapper ownerMapper;

    @Override
    protected ProcessResult doProcess(TaskContext context) {
        // 1. 解析参数
        var params = getParams(context);
        int warningDays = getInt(params, "warningDays", 30);
        int freezeDays = getInt(params, "freezeDays", 7);
        String ownerCode = getString(params, "ownerCode");

        // 2. 获取需要处理的货主列表
        List<Owner> owners;
        if (ownerCode != null && !ownerCode.isEmpty()) {
            Owner owner =
                    ownerMapper.selectOne(
                            new LambdaQueryWrapper<Owner>().eq(Owner::getOwnerCode, ownerCode));
            owners = owner != null ? List.of(owner) : List.of();
        } else {
            owners =
                    ownerMapper.selectList(
                            new LambdaQueryWrapper<Owner>().eq(Owner::getStatus, "ACTIVE"));
        }

        if (owners.isEmpty()) {
            return success("无活跃货主，跳过预警");
        }

        // 3. 按货主处理效期预警
        int totalWarning = 0;
        int totalFrozen = 0;
        LocalDate now = LocalDate.now();

        for (Owner owner : owners) {
            try {
                // TODO: 调用批次服务查询近效期批次
                // List<Batch> nearExpireBatches = batchService.getNearExpireBatches(
                //     owner.getOwnerCode(), now.plusDays(warningDays));
                //
                // for (Batch batch : nearExpireBatches) {
                //     if (batch.getExpireDate().isBefore(now.plusDays(freezeDays))) {
                //         // 7天内到期，自动冻结
                //         batchService.freezeBatch(batch.getBatchNo());
                //         totalFrozen++;
                //     }
                //     totalWarning++;
                // }
                //
                // // 发送预警通知
                // alertService.sendExpireWarning(owner, nearExpireBatches);

                log.info(
                        "效期预警处理: owner={}, warningDays={}, freezeDays={}",
                        owner.getOwnerCode(),
                        warningDays,
                        freezeDays);
            } catch (Exception e) {
                log.error("效期预警处理失败: owner={}, error={}", owner.getOwnerCode(), e.getMessage(), e);
            }
        }

        return success(
                String.format(
                        "效期预警完成: 预警批次=%d, 自动冻结=%d, 货主数=%d",
                        totalWarning, totalFrozen, owners.size()));
    }
}
