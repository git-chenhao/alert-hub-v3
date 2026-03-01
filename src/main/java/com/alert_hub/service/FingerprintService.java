package com.alert_hub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指纹服务
 *
 * 负责生成告警的唯一指纹，用于去重
 */
@Slf4j
@Service
public class FingerprintService {

    /**
     * 生成告警指纹
     * 使用 SHA256 算法：指纹 = hash(alertname + labels + severity)
     *
     * @param alertName 告警名称
     * @param labels 标签
     * @param severity 级别
     * @return 指纹（64位十六进制字符串）
     */
    public String generateFingerprint(String alertName, Map<String, String> labels, String severity) {
        try {
            // 构建指纹源字符串
            StringBuilder source = new StringBuilder();
            source.append(alertName != null ? alertName : "");
            source.append("|");

            // 对标签进行排序以保证一致性
            if (labels != null && !labels.isEmpty()) {
                String sortedLabels = labels.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(","));
                source.append(sortedLabels);
            }
            source.append("|");
            source.append(severity != null ? severity : "");

            // 计算 SHA256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.toString().getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String fingerprint = hexString.toString();
            log.debug("Generated fingerprint: {} for alert: {}", fingerprint, alertName);
            return fingerprint;

        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to generate fingerprint", e);
        }
    }

    /**
     * 生成分组键
     *
     * @param alertName 告警名称
     * @param labels 标签
     * @param groupBy 分组字段列表
     * @return 分组键
     */
    public String generateGroupKey(String alertName, Map<String, String> labels, String... groupBy) {
        StringBuilder key = new StringBuilder();

        // 默认包含告警名称
        key.append("alertname=").append(alertName != null ? alertName : "unknown");

        // 添加指定的分组字段
        if (labels != null && groupBy != null) {
            for (String field : groupBy) {
                if (labels.containsKey(field)) {
                    key.append(",").append(field).append("=").append(labels.get(field));
                }
            }
        }

        return key.toString();
    }

}
