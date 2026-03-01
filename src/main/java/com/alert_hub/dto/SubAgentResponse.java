package com.alert_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sub-Agent 分析响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubAgentResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 批次 ID
     */
    private String batchId;

    /**
     * 分析结果摘要
     */
    private String summary;

    /**
     * 根因分析
     */
    private String rootCause;

    /**
     * 建议操作
     */
    private String recommendation;

    /**
     * 错误信息
     */
    private String errorMessage;

}
