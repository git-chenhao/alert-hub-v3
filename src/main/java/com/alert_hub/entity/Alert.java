package com.alert_hub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警实体类
 *
 * 存储接收到的告警信息，包含指纹用于去重
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_source", columnList = "source")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警指纹，用于去重
     * 生成规则：SHA256(alertname + labels + severity)
     */
    @Column(name = "fingerprint", nullable = false, length = 64, unique = true)
    private String fingerprint;

    /**
     * 告警来源：prometheus, grafana, zabbix, generic
     */
    @Column(name = "source", nullable = false, length = 50)
    private String source;

    /**
     * 告警名称
     */
    @Column(name = "alert_name", nullable = false, length = 255)
    private String alertName;

    /**
     * 告警级别：critical, warning, info
     */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /**
     * 告警标签（JSON 格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels", columnDefinition = "TEXT")
    private Map<String, String> labels;

    /**
     * 告警注解（JSON 格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", columnDefinition = "TEXT")
    private Map<String, String> annotations;

    /**
     * 原始告警内容（JSON 格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_content", columnDefinition = "TEXT")
    private Object rawContent;

    /**
     * 告警状态：pending, firing, resolved
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    /**
     * 关联的批次 ID
     */
    @Column(name = "batch_id")
    private Long batchId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 持久化前自动设置时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
