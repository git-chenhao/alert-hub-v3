package com.alert_hub.dto;

import com.alert_hub.entity.AlertBatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警批次数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDTO {

    private Long id;
    private String groupKey;
    private Integer alertCount;
    private String status;
    private String analysisSummary;
    private String errorMessage;
    private LocalDateTime triggeredAt;
    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static BatchDTO fromEntity(AlertBatch batch) {
        return BatchDTO.builder()
                .id(batch.getId())
                .groupKey(batch.getGroupKey())
                .alertCount(batch.getAlertCount())
                .status(batch.getStatus())
                .analysisSummary(batch.getAnalysisSummary())
                .errorMessage(batch.getErrorMessage())
                .triggeredAt(batch.getTriggeredAt())
                .processingAt(batch.getProcessingAt())
                .completedAt(batch.getCompletedAt())
                .createdAt(batch.getCreatedAt())
                .build();
    }

    /**
     * 批次详情（包含告警列表）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detail {
        private BatchDTO batch;
        private java.util.List<AlertDTO> alerts;
    }

}
