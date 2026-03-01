package com.alert_hub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Prometheus Alertmanager Webhook 请求格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrometheusAlertDTO {

    /**
     * 告警状态：firing, resolved
     */
    private String status;

    /**
     * 告警列表
     */
    private List<Alert> alerts;

    /**
     * 分组标签
     */
    private Map<String, String> groupLabels;

    /**
     * 通用标签
     */
    private Map<String, String> commonLabels;

    /**
     * 通用注解
     */
    private Map<String, String> commonAnnotations;

    /**
     * 外部链接
     */
    private String externalURL;

    /**
     * 版本
     */
    private String version;

    /**
     * 接收服务
     */
    private String receiver;

    /**
     * 告警详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String status;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private String generatorURL;
        private String fingerprint;
    }

}
