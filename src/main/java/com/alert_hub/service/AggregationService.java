package com.alert_hub.service;

import com.alert_hub.dto.AlertDTO;
import com.alert_hub.dto.BatchDTO;
import com.alert_hub.dto.PageResult;
import com.alert_hub.dto.SubAgentRequest;
import com.alert_hub.dto.SubAgentResponse;
import com.alert_hub.entity.Alert;
import com.alert_hub.entity.AlertBatch;
import com.alert_hub.repository.AlertRepository;
import com.alert_hub.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 聚合服务
 *
 * 负责告警的攒批聚合和调度分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final BatchRepository batchRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${alert.aggregation.group-wait:30}")
    private int groupWait;

    @Value("${alert.aggregation.max-count:100}")
    private int maxCount;

    @Value("${alert.aggregation.max-wait:300}")
    private int maxWait;

    @Value("${alert.sub-agent.enabled:true}")
    private boolean subAgentEnabled;

    @Value("${alert.sub-agent.endpoints:http://localhost:8081/internal/analyze}")
    private String[] subAgentEndpoints;

    /**
     * 将告警加入聚合批次
     *
     * @param alert 告警
     * @return 批次
     */
    @Transactional
    public AlertBatch addToBatch(Alert alert) {
        // 生成分组键（按告警名称分组）
        String groupKey = "alertname:" + alert.getAlertName();

        // 查找或创建批次
        Optional<AlertBatch> existingBatch = batchRepository.findByGroupKeyAndStatus(groupKey, "PENDING");

        AlertBatch batch;
        if (existingBatch.isPresent()) {
            batch = existingBatch.get();
            batchRepository.incrementAlertCount(batch.getId(), 1);
            batch.setAlertCount(batch.getAlertCount() + 1);
            log.debug("Alert added to existing batch: batchId={}, alertCount={}", batch.getId(), batch.getAlertCount());
        } else {
            batch = AlertBatch.builder()
                    .groupKey(groupKey)
                    .alertCount(1)
                    .status("PENDING")
                    .triggeredAt(LocalDateTime.now())
                    .build();
            batch = batchRepository.save(batch);
            log.info("New batch created: batchId={}, groupKey={}", batch.getId(), groupKey);
        }

        // 更新告警的批次 ID
        alertRepository.updateBatchIdByIds(List.of(alert.getId()), batch.getId());

        return batch;
    }

    /**
     * 定时检查并触发批次
     * 每 10 秒执行一次
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkAndTriggerBatches() {
        // 计算触发时间点
        LocalDateTime triggerBefore = LocalDateTime.now().minusSeconds(groupWait);

        // 查找可触发的批次
        List<AlertBatch> batches = batchRepository.findTriggerableBatches(triggerBefore, maxCount);

        for (AlertBatch batch : batches) {
            try {
                triggerBatch(batch);
            } catch (Exception e) {
                log.error("Failed to trigger batch: batchId={}", batch.getId(), e);
            }
        }
    }

    /**
     * 触发批次进行根因分析
     *
     * @param batch 批次
     */
    @Transactional
    public void triggerBatch(AlertBatch batch) {
        log.info("Triggering batch: batchId={}, groupKey={}", batch.getId(), batch.getGroupKey());

        // 标记为处理中
        int updated = batchRepository.markAsProcessing(batch.getId());
        if (updated == 0) {
            log.warn("Batch already processed: batchId={}", batch.getId());
            return;
        }

        // 获取批次内的告警
        List<Alert> alerts = alertRepository.findByBatchId(batch.getId());
        if (alerts.isEmpty()) {
            log.warn("No alerts found in batch: batchId={}", batch.getId());
            batchRepository.markAsCompleted(batch.getId(), "No alerts to analyze");
            return;
        }

        // 调用 Sub-Agent 进行分析
        if (subAgentEnabled) {
            try {
                SubAgentResponse response = callSubAgent(batch.getId().toString(), alerts);

                if (response != null && response.isSuccess()) {
                    batchRepository.markAsCompleted(batch.getId(), response.getSummary());
                    log.info("Batch analysis completed: batchId={}, summary={}", batch.getId(), response.getSummary());

                    // 发送飞书通知
                    notificationService.sendFeishuNotification(batch, alerts, response);
                } else {
                    String error = response != null ? response.getErrorMessage() : "Unknown error";
                    batchRepository.markAsFailed(batch.getId(), error);
                    log.error("Batch analysis failed: batchId={}, error={}", batch.getId(), error);
                }
            } catch (Exception e) {
                batchRepository.markAsFailed(batch.getId(), e.getMessage());
                log.error("Failed to call Sub-Agent: batchId={}", batch.getId(), e);
            }
        } else {
            // Sub-Agent 未启用，直接标记完成
            String summary = String.format("Aggregated %d alerts (analysis disabled)", alerts.size());
            batchRepository.markAsCompleted(batch.getId(), summary);
            log.info("Batch completed without analysis: batchId={}", batch.getId());
        }
    }

    /**
     * 调用 Sub-Agent 进行分析
     *
     * @param batchId 批次 ID
     * @param alerts 告警列表
     * @return 分析响应
     */
    private SubAgentResponse callSubAgent(String batchId, List<Alert> alerts) {
        // 构建请求
        List<SubAgentRequest.AlertInfo> alertInfos = alerts.stream()
                .map(a -> SubAgentRequest.AlertInfo.builder()
                        .fingerprint(a.getFingerprint())
                        .alertName(a.getAlertName())
                        .severity(a.getSeverity())
                        .source(a.getSource())
                        .labels(a.getLabels())
                        .annotations(a.getAnnotations())
                        .build())
                .toList();

        SubAgentRequest request = SubAgentRequest.builder()
                .batchId(batchId)
                .alerts(alertInfos)
                .build();

        // 轮询多个端点（简单负载均衡）
        for (String endpoint : subAgentEndpoints) {
            try {
                String url = endpoint.trim();
                log.debug("Calling Sub-Agent: url={}, batchId={}", url, batchId);

                SubAgentResponse response = restTemplate.postForObject(url, request, SubAgentResponse.class);

                if (response != null) {
                    log.info("Sub-Agent response: batchId={}, success={}", batchId, response.isSuccess());
                    return response;
                }
            } catch (RestClientException e) {
                log.warn("Failed to call Sub-Agent endpoint: endpoint={}, error={}", endpoint, e.getMessage());
            }
        }

        log.error("All Sub-Agent endpoints failed for batch: {}", batchId);
        return SubAgentResponse.builder()
                .success(false)
                .batchId(batchId)
                .errorMessage("All Sub-Agent endpoints failed")
                .build();
    }

    /**
     * 查询批次
     *
     * @param batchId 批次 ID
     * @return 批次
     */
    public Optional<AlertBatch> findBatchById(Long batchId) {
        return batchRepository.findById(batchId);
    }

    /**
     * 分页查询批次
     *
     * @param page 页码
     * @param size 每页大小
     * @param status 状态（可选）
     * @return 分页结果
     */
    public PageResult<BatchDTO> queryBatches(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<AlertBatch> batchPage;
        if (status != null && !status.isEmpty()) {
            batchPage = batchRepository.findByStatus(status, pageable);
        } else {
            batchPage = batchRepository.findAll(pageable);
        }

        List<BatchDTO> items = batchPage.getContent().stream()
                .map(BatchDTO::fromEntity)
                .toList();

        return PageResult.of(items, batchPage.getTotalElements(), page, size);
    }

    /**
     * 获取批次详情（包含告警列表）
     *
     * @param batchId 批次 ID
     * @return 批次详情
     */
    public BatchDTO.Detail getBatchDetail(Long batchId) {
        Optional<AlertBatch> batchOpt = batchRepository.findById(batchId);
        if (batchOpt.isEmpty()) {
            return null;
        }

        AlertBatch batch = batchOpt.get();
        List<Alert> alerts = alertRepository.findByBatchId(batchId);

        List<AlertDTO> alertDTOs = alerts.stream()
                .map(AlertDTO::fromEntity)
                .toList();

        return BatchDTO.Detail.builder()
                .batch(BatchDTO.fromEntity(batch))
                .alerts(alertDTOs)
                .build();
    }

    /**
     * 统计批次数量
     *
     * @param status 状态（可选）
     * @return 数量
     */
    public long countBatches(String status) {
        if (status != null && !status.isEmpty()) {
            return batchRepository.countByStatus(status);
        }
        return batchRepository.count();
    }

}
