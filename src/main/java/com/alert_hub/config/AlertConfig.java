package com.alert_hub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 告警配置
 *
 * 从 application.yml 读取告警相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert.aggregation")
public class AlertConfig {

    /**
     * 等待多少秒后触发聚合
     */
    private int groupWait = 30;

    /**
     * 最大告警数触发聚合
     */
    private int maxCount = 100;

    /**
     * 最大等待时间（秒）
     */
    private int maxWait = 300;

    /**
     * 去重窗口时间（秒）
     */
    private int dedupWindow = 300;

}
