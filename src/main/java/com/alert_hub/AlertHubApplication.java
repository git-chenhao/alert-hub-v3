package com.alert_hub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Alert Hub V3 - 统一告警聚合平台
 *
 * 核心功能：
 * 1. 接收来自各告警系统的 Webhook
 * 2. 基于指纹进行告警去重
 * 3. 窗口策略攒批聚合
 * 4. 调度 Sub-Agent 进行根因分析
 * 5. 推送飞书通知
 *
 * @author Alert Hub Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class AlertHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertHubApplication.class, args);
    }

}
