# Alert Hub V3 - 统一告警聚合平台

统一接收来自各告警系统的 Webhook，完成告警聚合后再调度各 Sub-Agent 进行根因分析。

## 核心功能

### 1. Webhook 接口
- `POST /api/webhook/alerts` - Prometheus Alertmanager 格式
- `POST /api/webhook/grafana` - Grafana 格式
- `POST /api/webhook/zabbix` - Zabbix 格式
- `POST /api/webhook/generic` - 通用 JSON 格式
- `GET /api/webhook/health` - 健康检查

### 2. 去重机制
- 基于告警内容生成唯一指纹（fingerprint）
- 使用 SHA256 算法：指纹 = hash(alertname + labels + severity)
- 支持配置去重窗口时间（默认 5 分钟）

### 3. 攒批聚合
- 按告警名称分组
- 配置项：
  - `group_wait`: 等待多少秒后触发聚合（默认 30s）
  - `max_count`: 最大告警数触发聚合（默认 100）
  - `max_wait`: 最大等待时间（默认 5min）
- 批次状态流转：PENDING → PROCESSING → COMPLETED/FAILED

### 4. 调度分析
- 聚合批次触发后调用 Sub-Agent
- 接口：POST /internal/analyze
- 支持配置多个 Sub-Agent 端点（负载均衡）

### 5. 飞书通知
- 分析完成后推送飞书 Webhook
- 卡片格式：蓝色主题，包含批次 ID、告警数、分析结果摘要

## 技术栈
- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- H2 Database（开发）/ MySQL（生产）
- Lombok
- Maven

## 快速开始

### 1. 构建项目
```bash
mvn clean package -DskipTests
```

### 2. 运行（开发模式）
```bash
java -jar target/alert-hub-v3-1.0.0-SNAPSHOT.jar
```

或使用 Maven：
```bash
mvn spring-boot:run
```

### 3. 访问 H2 控制台（开发环境）
- URL: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:alerthub
- Username: sa
- Password: (空)

## API 文档

### Webhook 接口

#### Prometheus Alertmanager
```bash
curl -X POST http://localhost:8080/api/webhook/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "status": "firing",
    "alerts": [{
      "status": "firing",
      "labels": {"alertname": "HighCPU", "severity": "critical"},
      "annotations": {"summary": "CPU usage > 90%"}
    }]
  }'
```

#### Grafana
```bash
curl -X POST http://localhost:8080/api/webhook/grafana \
  -H "Content-Type: application/json" \
  -d '{
    "ruleName": "High Memory Usage",
    "state": "alerting",
    "title": "Memory Alert",
    "message": "Memory usage exceeded threshold"
  }'
```

#### 通用格式
```bash
curl -X POST http://localhost:8080/api/webhook/generic \
  -H "Content-Type: application/json" \
  -d '{
    "alertName": "ServiceDown",
    "severity": "critical",
    "labels": {"service": "api-gateway"},
    "annotations": {"description": "API Gateway is not responding"}
  }'
```

### 管理接口

#### 查询告警列表
```bash
curl http://localhost:8080/admin/alerts?page=1&size=20
```

#### 查询批次列表
```bash
curl http://localhost:8080/admin/batches?page=1&size=20
```

#### 查询批次详情
```bash
curl http://localhost:8080/admin/batches/1
```

## 配置说明

### application.yml 关键配置
```yaml
alert:
  aggregation:
    group-wait: 30      # 等待多少秒后触发聚合
    max-count: 100      # 最大告警数触发聚合
    max-wait: 300       # 最大等待时间（秒）
    dedup-window: 300   # 去重窗口时间（秒）

  sub-agent:
    enabled: true
    endpoints:
      - http://localhost:8081/internal/analyze

  notification:
    feishu:
      enabled: false
      webhook-url: ${FEISHU_WEBHOOK_URL:}
```

### 生产环境配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:alert_hub}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}

alert:
  notification:
    feishu:
      enabled: true
      webhook-url: ${FEISHU_WEBHOOK_URL}
```

## 项目结构
```
src/main/java/com/alert_hub/
├── AlertHubApplication.java    # 主应用入口
├── config/                      # 配置类
│   ├── AlertConfig.java
│   ├── JpaConfig.java
│   └── WebConfig.java
├── controller/                  # 控制器
│   ├── AdminController.java
│   └── WebhookController.java
├── dto/                         # 数据传输对象
├── entity/                      # 实体类
│   ├── Alert.java
│   └── AlertBatch.java
├── exception/                   # 异常处理
├── repository/                  # 数据访问层
└── service/                     # 业务逻辑层
    ├── AggregationService.java
    ├── AlertService.java
    ├── FingerprintService.java
    └── NotificationService.java
```

## License
MIT
