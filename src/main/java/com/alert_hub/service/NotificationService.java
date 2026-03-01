package com.alert_hub.service;

import com.alert_hub.dto.SubAgentResponse;
import com.alert_hub.entity.Alert;
import com.alert_hub.entity.AlertBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知服务
 *
 * 负责发送飞书等通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    @Value("${alert.notification.feishu.enabled:false}")
    private boolean feishuEnabled;

    @Value("${alert.notification.feishu.webhook-url:}")
    private String feishuWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送飞书通知
     *
     * @param batch 批次
     * @param alerts 告警列表
     * @param response 分析响应
     */
    public void sendFeishuNotification(AlertBatch batch, List<Alert> alerts, SubAgentResponse response) {
        if (!feishuEnabled || feishuWebhookUrl == null || feishuWebhookUrl.isEmpty()) {
            log.debug("Feishu notification disabled or webhook URL not configured");
            return;
        }

        try {
            // 构建飞书卡片消息
            Map<String, Object> card = buildFeishuCard(batch, alerts, response);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(card, headers);

            restTemplate.postForObject(feishuWebhookUrl, request, String.class);
            log.info("Feishu notification sent: batchId={}", batch.getId());

        } catch (RestClientException e) {
            log.error("Failed to send Feishu notification: batchId={}", batch.getId(), e);
        }
    }

    /**
     * 构建飞书卡片
     *
     * @param batch 批次
     * @param alerts 告警列表
     * @param response 分析响应
     * @return 卡片内容
     */
    private Map<String, Object> buildFeishuCard(AlertBatch batch, List<Alert> alerts, SubAgentResponse response) {
        // 统计告警级别
        Map<String, Long> severityCount = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getSeverity, Collectors.counting()));

        // 构建卡片
        Map<String, Object> card = new HashMap<>();
        card.put("msg_type", "interactive");

        Map<String, Object> content = new HashMap<>();

        // 卡片配置
        Map<String, Object> config = new HashMap<>();
        config.put("wide_screen_mode", true);
        content.put("config", config);

        // 卡片标题
        Map<String, Object> header = new HashMap<>();
        Map<String, Object> title = new HashMap<>();
        title.put("tag", "plain_text");
        title.put("content", "🚨 告警聚合分析报告");
        header.put("title", title);
        header.put("template", "blue");
        content.put("header", header);

        // 卡片元素
        List<Map<String, Object>> elements = new java.util.ArrayList<>();

        // 基本信息模块
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("tag", "div");
        Map<String, Object> basicFields = new HashMap<>();
        basicFields.put("tag", "lark_md");
        basicFields.put("content", String.format(
                "**批次 ID**: %s\n" +
                "**告警数量**: %d\n" +
                "**状态**: %s\n" +
                "**触发时间**: %s",
                batch.getId(),
                batch.getAlertCount(),
                batch.getStatus(),
                batch.getTriggeredAt()
        ));
        basicInfo.put("fields", List.of(basicFields));
        elements.add(basicInfo);

        // 告警级别统计
        Map<String, Object> severityInfo = new HashMap<>();
        severityInfo.put("tag", "div");
        Map<String, Object> severityFields = new HashMap<>();
        severityFields.put("tag", "lark_md");
        severityFields.put("content", String.format(
                "**告警级别分布**:\n" +
                "- 🔴 Critical: %d\n" +
                "- 🟡 Warning: %d\n" +
                "- 🟢 Info: %d",
                severityCount.getOrDefault("critical", 0L),
                severityCount.getOrDefault("warning", 0L),
                severityCount.getOrDefault("info", 0L)
        ));
        severityInfo.put("fields", List.of(severityFields));
        elements.add(severityInfo);

        // 分析结果
        if (response != null && response.getSummary() != null) {
            Map<String, Object> analysisInfo = new HashMap<>();
            analysisInfo.put("tag", "div");
            Map<String, Object> analysisFields = new HashMap<>();
            analysisFields.put("tag", "lark_md");
            analysisFields.put("content", String.format(
                    "**分析结果**:\n%s",
                    response.getSummary()
            ));
            analysisInfo.put("fields", List.of(analysisFields));
            elements.add(analysisInfo);
        }

        // 分割线
        Map<String, Object> divider = new HashMap<>();
        divider.put("tag", "hr");
        elements.add(divider);

        content.put("elements", elements);
        card.put("card", content);

        return card;
    }

}
