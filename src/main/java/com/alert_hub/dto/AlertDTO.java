package com.alert_hub.dto;

import com.alert_hub.entity.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDTO {

    private Long id;
    private String fingerprint;
    private String source;
    private String alertName;
    private String severity;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private String status;
    private Long batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static AlertDTO fromEntity(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .source(alert.getSource())
                .alertName(alert.getAlertName())
                .severity(alert.getSeverity())
                .labels(alert.getLabels())
                .annotations(alert.getAnnotations())
                .status(alert.getStatus())
                .batchId(alert.getBatchId())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

    /**
     * 创建告警请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String source;
        private String alertName;
        private String severity;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private Object rawContent;
    }

}
