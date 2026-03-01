package com.alert_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Sub-Agent 分析请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAgentRequest {

    /**
     * 批次 ID
     */
    private String batchId;

    /**
     * 告警列表
     */
    private List<AlertInfo> alerts;

    /**
     * 告警信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertInfo {
        private String fingerprint;
        private String alertName;
        private String severity;
        private String source;
        private Map<String, String> labels;
        private Map<String, String> annotations;
    }

}
