package com.alert_hub.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Grafana Webhook 请求格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrafanaAlertDTO {

    /**
     * 告警 ID
     */
    private String id;

    /**
     * 规则 ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则 URL
     */
    private String ruleUrl;

    /**
     * 状态：alerting, ok
     */
    private String state;

    /**
     * 标题
     */
    private String title;

    /**
     * 消息
     */
    private String message;

    /**
     * 标签
     */
    private Map<String, String> tags;

    /**
     * 开始时间
     */
    private String startsAt;

    /**
     * 结束时间
     */
    private String endsAt;

    /**
     * 评估数据
     */
    private List<EvalMatch> evalMatches;

    /**
     * 面板 ID
     */
    private Long panelId;

    /**
     * 仪表盘 ID
     */
    private Long dashboardId;

    /**
     * 评估匹配项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvalMatch {
        private String metric;
        private Double value;
        private Map<String, Object> tags;
    }

}
