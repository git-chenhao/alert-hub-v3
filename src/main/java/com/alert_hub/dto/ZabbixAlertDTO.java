package com.alert_hub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Zabbix Webhook 请求格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZabbixAlertDTO {

    /**
     * 事件 ID
     */
    private String eventId;

    /**
     * 触发器 ID
     */
    private String triggerId;

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 主机 IP
     */
    private String hostIp;

    /**
     * 触发器名称（告警名称）
     */
    private String triggerName;

    /**
     * 告警级别
     */
    private String severity;

    /**
     * 状态：PROBLEM, OK
     */
    private String status;

    /**
     * 事件时间
     */
    private String eventTime;

    /**
     * 告警内容
     */
    private String message;

    /**
     * 标签
     */
    private Map<String, String> tags;

    /**
     * 值
     */
    private String value;

    /**
     * 项目名称
     */
    private String itemName;

}
