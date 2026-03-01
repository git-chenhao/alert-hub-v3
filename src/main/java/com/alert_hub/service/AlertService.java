package com.alert_hub.service;

import com.alert_hub.dto.AlertDTO;
import com.alert_hub.dto.PageResult;
import com.alert_hub.entity.Alert;
import com.alert_hub.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 告警服务
 *
 * 处理告警的接收、存储、查询等核心操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final FingerprintService fingerprintService;

    /**
     * 接收并保存告警
     *
     * @param source 告警来源
     * @param alertName 告警名称
     * @param severity 告警级别
     * @param labels 标签
     * @param annotations 注解
     * @param rawContent 原始内容
     * @return 保存的告警，如果重复则返回 null
     */
    @Transactional
    public Alert receiveAlert(String source, String alertName, String severity,
                              Map<String, String> labels, Map<String, String> annotations,
                              Object rawContent) {
        // 生成指纹
        String fingerprint = fingerprintService.generateFingerprint(alertName, labels, severity);

        // 检查是否重复（5分钟窗口内）
        LocalDateTime dedupWindow = LocalDateTime.now().minusMinutes(5);
        if (alertRepository.existsByFingerprintAndCreatedAtAfter(fingerprint, dedupWindow)) {
            log.info("Duplicate alert detected, fingerprint: {}, alertName: {}", fingerprint, alertName);
            return null;
        }

        // 创建告警
        Alert alert = Alert.builder()
                .fingerprint(fingerprint)
                .source(source)
                .alertName(alertName)
                .severity(severity != null ? severity : "info")
                .labels(labels)
                .annotations(annotations)
                .rawContent(rawContent)
                .status("pending")
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("Alert saved: id={}, fingerprint={}, alertName={}", savedAlert.getId(), fingerprint, alertName);

        return savedAlert;
    }

    /**
     * 根据指纹查询告警
     *
     * @param fingerprint 指纹
     * @return 告警
     */
    public Optional<Alert> findByFingerprint(String fingerprint) {
        return alertRepository.findByFingerprint(fingerprint);
    }

    /**
     * 根据 ID 查询告警
     *
     * @param id ID
     * @return 告警
     */
    public Optional<Alert> findById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * 分页查询告警
     *
     * @param page 页码
     * @param size 每页大小
     * @param status 状态（可选）
     * @param source 来源（可选）
     * @return 分页结果
     */
    public PageResult<AlertDTO> queryAlerts(int page, int size, String status, String source) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Alert> alertPage;
        if (status != null && !status.isEmpty()) {
            alertPage = alertRepository.findByStatus(status, pageable);
        } else if (source != null && !source.isEmpty()) {
            alertPage = alertRepository.findBySource(source, pageable);
        } else {
            alertPage = alertRepository.findAll(pageable);
        }

        List<AlertDTO> items = alertPage.getContent().stream()
                .map(AlertDTO::fromEntity)
                .toList();

        return PageResult.of(items, alertPage.getTotalElements(), page, size);
    }

    /**
     * 查询未分批的告警
     *
     * @return 告警列表
     */
    public List<Alert> findUnbatchedAlerts() {
        return alertRepository.findUnbatchedAlerts();
    }

    /**
     * 查询批次内的告警
     *
     * @param batchId 批次 ID
     * @return 告警列表
     */
    public List<Alert> findByBatchId(Long batchId) {
        return alertRepository.findByBatchId(batchId);
    }

    /**
     * 批量更新告警的批次 ID
     *
     * @param ids 告警 ID 列表
     * @param batchId 批次 ID
     * @return 更新数量
     */
    @Transactional
    public int updateBatchId(List<Long> ids, Long batchId) {
        return alertRepository.updateBatchIdByIds(ids, batchId);
    }

    /**
     * 统计告警数量
     *
     * @param status 状态（可选）
     * @return 数量
     */
    public long countAlerts(String status) {
        if (status != null && !status.isEmpty()) {
            return alertRepository.countByStatus(status);
        }
        return alertRepository.count();
    }

    /**
     * 清理过期告警
     *
     * @param days 保留天数
     * @return 删除数量
     */
    @Transactional
    public int cleanOldAlerts(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        int deleted = alertRepository.deleteByCreatedAtBefore(before);
        log.info("Cleaned {} alerts older than {} days", deleted, days);
        return deleted;
    }

}
