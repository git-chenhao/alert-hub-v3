package com.alert_hub.controller;

import com.alert_hub.dto.AlertDTO;
import com.alert_hub.dto.ApiResponse;
import com.alert_hub.dto.BatchDTO;
import com.alert_hub.dto.PageResult;
import com.alert_hub.entity.Alert;
import com.alert_hub.entity.AlertBatch;
import com.alert_hub.service.AggregationService;
import com.alert_hub.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台控制器
 *
 * 提供告警和批次的管理查询接口
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AlertService alertService;
    private final AggregationService aggregationService;

    /**
     * 获取仪表盘统计
     */
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 告警统计
        dashboard.put("totalAlerts", alertService.countAlerts(null));
        dashboard.put("pendingAlerts", alertService.countAlerts("pending"));
        dashboard.put("batchedAlerts", alertService.countAlerts("batched"));

        // 批次统计
        dashboard.put("totalBatches", aggregationService.countBatches(null));
        dashboard.put("pendingBatches", aggregationService.countBatches("PENDING"));
        dashboard.put("processingBatches", aggregationService.countBatches("PROCESSING"));
        dashboard.put("completedBatches", aggregationService.countBatches("COMPLETED"));
        dashboard.put("failedBatches", aggregationService.countBatches("FAILED"));

        return ApiResponse.success(dashboard);
    }

    // ==================== 告警管理 ====================

    /**
     * 查询告警列表
     *
     * @param page 页码（默认 1）
     * @param size 每页大小（默认 20）
     * @param status 状态过滤（可选）
     * @param source 来源过滤（可选）
     */
    @GetMapping("/alerts")
    public ApiResponse<PageResult<AlertDTO>> listAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {

        PageResult<AlertDTO> result = alertService.queryAlerts(page, size, status, source);
        return ApiResponse.success(result);
    }

    /**
     * 查询告警详情
     *
     * @param id 告警 ID
     */
    @GetMapping("/alerts/{id}")
    public ApiResponse<AlertDTO> getAlert(@PathVariable Long id) {
        return alertService.findById(id)
                .map(AlertDTO::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Alert not found"));
    }

    /**
     * 根据指纹查询告警
     *
     * @param fingerprint 指纹
     */
    @GetMapping("/alerts/fingerprint/{fingerprint}")
    public ApiResponse<AlertDTO> getAlertByFingerprint(@PathVariable String fingerprint) {
        return alertService.findByFingerprint(fingerprint)
                .map(AlertDTO::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Alert not found"));
    }

    /**
     * 清理过期告警
     *
     * @param days 保留天数
     */
    @PostMapping("/alerts/cleanup")
    public ApiResponse<Map<String, Object>> cleanupAlerts(@RequestParam(defaultValue = "30") int days) {
        int deleted = alertService.cleanOldAlerts(days);

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        result.put("retentionDays", days);

        return ApiResponse.success("Cleanup completed", result);
    }

    // ==================== 批次管理 ====================

    /**
     * 查询批次列表
     *
     * @param page 页码（默认 1）
     * @param size 每页大小（默认 20）
     * @param status 状态过滤（可选）
     */
    @GetMapping("/batches")
    public ApiResponse<PageResult<BatchDTO>> listBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        PageResult<BatchDTO> result = aggregationService.queryBatches(page, size, status);
        return ApiResponse.success(result);
    }

    /**
     * 查询批次详情
     *
     * @param id 批次 ID
     */
    @GetMapping("/batches/{id}")
    public ApiResponse<BatchDTO.Detail> getBatch(@PathVariable Long id) {
        BatchDTO.Detail detail = aggregationService.getBatchDetail(id);
        if (detail == null) {
            return ApiResponse.error(404, "Batch not found");
        }
        return ApiResponse.success(detail);
    }

    /**
     * 手动触发批次
     *
     * @param id 批次 ID
     */
    @PostMapping("/batches/{id}/trigger")
    public ApiResponse<Map<String, Object>> triggerBatch(@PathVariable Long id) {
        return aggregationService.findBatchById(id)
                .map(batch -> {
                    try {
                        aggregationService.triggerBatch(batch);
                        Map<String, Object> result = new HashMap<>();
                        result.put("batchId", id);
                        result.put("triggered", true);
                        return ApiResponse.success("Batch triggered", result);
                    } catch (Exception e) {
                        log.error("Failed to trigger batch: {}", id, e);
                        return ApiResponse.<Map<String, Object>>error(500, "Failed to trigger batch: " + e.getMessage());
                    }
                })
                .orElse(ApiResponse.error(404, "Batch not found"));
    }

    // ==================== 配置管理 ====================

    /**
     * 获取当前配置
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();

        // 聚合配置
        Map<String, Object> aggregation = new HashMap<>();
        aggregation.put("note", "Configuration values from application.yml");
        config.put("aggregation", aggregation);

        return ApiResponse.success(config);
    }

    // ==================== 统计接口 ====================

    /**
     * 按来源统计告警
     */
    @GetMapping("/stats/by-source")
    public ApiResponse<Map<String, Long>> statsBySource() {
        // 简化实现：返回总数
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", alertService.countAlerts(null));
        return ApiResponse.success(stats);
    }

    /**
     * 按级别统计告警
     */
    @GetMapping("/stats/by-severity")
    public ApiResponse<Map<String, Long>> statsBySeverity() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("critical", alertService.countAlerts("critical"));
        stats.put("warning", alertService.countAlerts("warning"));
        stats.put("info", alertService.countAlerts("info"));
        return ApiResponse.success(stats);
    }

}
