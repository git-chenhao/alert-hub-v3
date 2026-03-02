package com.alert_hub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 配置
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.alert_hub.repository")
@EnableTransactionManagement
public class JpaConfig {

}
