package com.alert_hub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警聚合批次实体类
 *
 * 管理告警的攒批聚合状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alert_batches", indexes = {
    @Index(name = "idx_group_key", columnList = "group_key"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_triggered_at", columnList = "triggered_at")
})
public class AlertBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分组键（如 alertname + service）
     */
    @Column(name = "group_key", nullable = false, length = 255)
    private String groupKey;

    /**
     * 批次内告警数量
     */
    @Column(name = "alert_count", nullable = false)
    @Builder.Default
    private Integer alertCount = 0;

    /**
     * 批次状态：PENDING, PROCESSING, COMPLETED, FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * 分析结果摘要
     */
    @Column(name = "analysis_summary", columnDefinition = "TEXT")
    private String analysisSummary;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 触发时间
     */
    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    /**
     * 处理开始时间
     */
    @Column(name = "processing_at")
    private LocalDateTime processingAt;

    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
        if (triggeredAt == null) {
            triggeredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
