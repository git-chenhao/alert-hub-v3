package com.alert_hub.controller;

import com.alert_hub.dto.ApiResponse;
import com.alert_hub.dto.GrafanaAlertDTO;
import com.alert_hub.dto.PrometheusAlertDTO;
import com.alert_hub.dto.ZabbixAlertDTO;
import com.alert_hub.entity.Alert;
import com.alert_hub.service.AggregationService;
import com.alert_hub.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Webhook 控制器
 *
 * 接收来自各告警系统的 Webhook 请求
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final AlertService alertService;
    private final AggregationService aggregationService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "alert-hub-v3");
        health.put("timestamp", System.currentTimeMillis());

        long alertCount = alertService.countAlerts(null);
        long batchCount = aggregationService.countBatches(null);
        health.put("alertCount", alertCount);
        health.put("batchCount", batchCount);

        return ApiResponse.success(health);
    }

    /**
     * 接收 Prometheus Alertmanager Webhook
     * 格式：https://prometheus.io/docs/alerting/latest/configuration/#webhook_config
     */
    @PostMapping("/alerts")
    public ApiResponse<Map<String, Object>> receivePrometheusAlert(
            @RequestBody PrometheusAlertDTO prometheusAlert,
            @RequestHeader(value = "X-Alert-Source", required = false) String sourceHeader) {

        log.info("Received Prometheus alert: status={}, count={}",
                prometheusAlert.getStatus(),
                prometheusAlert.getAlerts() != null ? prometheusAlert.getAlerts().size() : 0);

        int received = 0;
        int duplicates = 0;

        if (prometheusAlert.getAlerts() != null) {
            for (PrometheusAlertDTO.Alert alert : prometheusAlert.getAlerts()) {
                String alertName = alert.getLabels() != null ?
                        alert.getLabels().getOrDefault("alertname", "unknown") : "unknown";
                String severity = alert.getLabels() != null ?
                        alert.getLabels().getOrDefault("severity", "info") : "info";

                Alert savedAlert = alertService.receiveAlert(
                        "prometheus",
                        alertName,
                        severity,
                        alert.getLabels(),
                        alert.getAnnotations(),
                        alert
                );

                if (savedAlert != null) {
                    received++;
                    // 加入聚合批次
                    aggregationService.addToBatch(savedAlert);
                } else {
                    duplicates++;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("received", received);
        result.put("duplicates", duplicates);
        result.put("status", prometheusAlert.getStatus());

        return ApiResponse.success("Prometheus alerts processed", result);
    }

    /**
     * 接收 Grafana Webhook
     * 格式：https://grafana.com/docs/grafana/latest/alerting/old-alerting/notifications/#webhook
     */
    @PostMapping("/grafana")
    public ApiResponse<Map<String, Object>> receiveGrafanaAlert(
            @RequestBody GrafanaAlertDTO grafanaAlert) {

        log.info("Received Grafana alert: ruleName={}, state={}",
                grafanaAlert.getRuleName(), grafanaAlert.getState());

        // 构建标签
        Map<String, String> labels = new HashMap<>();
        if (grafanaAlert.getTags() != null) {
            labels.putAll(grafanaAlert.getTags());
        }
        labels.put("ruleId", String.valueOf(grafanaAlert.getRuleId()));
        labels.put("dashboardId", String.valueOf(grafanaAlert.getDashboardId()));
        labels.put("panelId", String.valueOf(grafanaAlert.getPanelId()));

        // 构建注解
        Map<String, String> annotations = new HashMap<>();
        annotations.put("title", grafanaAlert.getTitle());
        annotations.put("message", grafanaAlert.getMessage());
        annotations.put("ruleUrl", grafanaAlert.getRuleUrl());

        // 确定告警级别
        String severity = "firing".equals(grafanaAlert.getState()) ? "warning" : "info";

        Alert savedAlert = alertService.receiveAlert(
                "grafana",
                grafanaAlert.getRuleName(),
                severity,
                labels,
                annotations,
                grafanaAlert
        );

        Map<String, Object> result = new HashMap<>();
        result.put("received", savedAlert != null ? 1 : 0);
        result.put("duplicates", savedAlert == null ? 1 : 0);
        result.put("ruleName", grafanaAlert.getRuleName());
        result.put("state", grafanaAlert.getState());

        if (savedAlert != null) {
            aggregationService.addToBatch(savedAlert);
        }

        return ApiResponse.success("Grafana alert processed", result);
    }

    /**
     * 接收 Zabbix Webhook
     */
    @PostMapping("/zabbix")
    public ApiResponse<Map<String, Object>> receiveZabbixAlert(
            @RequestBody ZabbixAlertDTO zabbixAlert) {

        log.info("Received Zabbix alert: triggerName={}, status={}",
                zabbixAlert.getTriggerName(), zabbixAlert.getStatus());

        // 构建标签
        Map<String, String> labels = new HashMap<>();
        if (zabbixAlert.getTags() != null) {
            labels.putAll(zabbixAlert.getTags());
        }
        labels.put("eventId", zabbixAlert.getEventId());
        labels.put("triggerId", zabbixAlert.getTriggerId());
        labels.put("hostname", zabbixAlert.getHostname());
        labels.put("hostIp", zabbixAlert.getHostIp());
        labels.put("itemName", zabbixAlert.getItemName());

        // 构建注解
        Map<String, String> annotations = new HashMap<>();
        annotations.put("message", zabbixAlert.getMessage());
        annotations.put("value", zabbixAlert.getValue());
        annotations.put("eventTime", zabbixAlert.getEventTime());

        // 确定告警级别
        String severity = mapZabbixSeverity(zabbixAlert.getSeverity());
        // Zabbix 状态：PROBLEM 或 OK
        String status = "PROBLEM".equals(zabbixAlert.getStatus()) ? "firing" : "resolved";

        Alert savedAlert = alertService.receiveAlert(
                "zabbix",
                zabbixAlert.getTriggerName(),
                severity,
                labels,
                annotations,
                zabbixAlert
        );

        Map<String, Object> result = new HashMap<>();
        result.put("received", savedAlert != null ? 1 : 0);
        result.put("duplicates", savedAlert == null ? 1 : 0);
        result.put("triggerName", zabbixAlert.getTriggerName());
        result.put("status", status);

        if (savedAlert != null) {
            aggregationService.addToBatch(savedAlert);
        }

        return ApiResponse.success("Zabbix alert processed", result);
    }

    /**
     * 接收通用 JSON 格式 Webhook
     */
    @PostMapping("/generic")
    public ApiResponse<Map<String, Object>> receiveGenericAlert(
            @RequestBody Map<String, Object> genericAlert) {

        log.info("Received generic alert: {}", genericAlert.keySet());

        // 提取通用字段
        String alertName = (String) genericAlert.getOrDefault("alertName",
                genericAlert.getOrDefault("name", "unknown"));
        String severity = (String) genericAlert.getOrDefault("severity",
                genericAlert.getOrDefault("level", "info"));

        // 提取标签
        @SuppressWarnings("unchecked")
        Map<String, String> labels = (Map<String, String>) genericAlert.getOrDefault("labels",
                genericAlert.getOrDefault("tags", new HashMap<>()));

        // 提取注解
        @SuppressWarnings("unchecked")
        Map<String, String> annotations = (Map<String, String>) genericAlert.getOrDefault("annotations",
                genericAlert.getOrDefault("details", new HashMap<>()));

        Alert savedAlert = alertService.receiveAlert(
                "generic",
                alertName,
                severity,
                labels,
                annotations,
                genericAlert
        );

        Map<String, Object> result = new HashMap<>();
        result.put("received", savedAlert != null ? 1 : 0);
        result.put("duplicates", savedAlert == null ? 1 : 0);
        result.put("alertName", alertName);

        if (savedAlert != null) {
            aggregationService.addToBatch(savedAlert);
        }

        return ApiResponse.success("Generic alert processed", result);
    }

    /**
     * 映射 Zabbix 告警级别
     */
    private String mapZabbixSeverity(String zabbixSeverity) {
        if (zabbixSeverity == null) {
            return "info";
        }
        return switch (zabbixSeverity.toLowerCase()) {
            case "disaster", "high" -> "critical";
            case "average", "warning" -> "warning";
            default -> "info";
        };
    }

}
