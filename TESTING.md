# Story Weaver 测试文档

## 概述

本文档描述了 Story Weaver 项目的测试策略、测试用例和测试执行指南。根据项目规范文档，系统需要确保核心功能正常运行，包括用户认证、项目管理、章节管理、AI续写、因果管理、RAG功能等。

## 测试环境要求

### 硬件要求
- CPU: 4核以上
- 内存: 8GB以上
- 磁盘空间: 10GB以上

### 软件要求
- Java 21+
- Node.js 20+
- MySQL 8.0+
- Maven 3.8+
- Redis 6.0+ (可选)

### 网络要求
- 可访问外部AI API服务（如OpenAI、DeepSeek等）
- 稳定的网络连接

## 测试类型

### 1. 单元测试
- **范围**: 单个类或方法
- **工具**: JUnit 5, Mockito
- **目标**: 验证业务逻辑正确性

### 2. 集成测试
- **范围**: 模块间集成
- **工具**: Spring Boot Test, TestContainers
- **目标**: 验证接口调用和数据流转

### 3. API测试
- **范围**: REST API接口
- **工具**: RestAssured, Postman
- **目标**: 验证API功能和性能

### 4. 端到端测试
- **范围**: 完整用户流程
- **工具**: Cypress, Playwright
- **目标**: 验证用户交互流程

### 5. 性能测试
- **范围**: 系统性能
- **工具**: JMeter, Gatling
- **目标**: 验证系统负载能力

## 测试用例矩阵

### 认证模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| AUTH-001 | 用户登录成功 | 高 | 单元/API |
| AUTH-002 | 用户登录失败（密码错误） | 高 | 单元/API |
| AUTH-003 | 用户注册成功 | 高 | 单元/API |
| AUTH-004 | 用户注册失败（用户名已存在） | 高 | 单元/API |
| AUTH-005 | JWT令牌验证 | 高 | 单元/API |
| AUTH-006 | 令牌刷新 | 中 | API |

### 项目管理模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| PROJ-001 | 创建新项目 | 高 | 单元/API |
| PROJ-002 | 获取项目列表 | 高 | 单元/API |
| PROJ-003 | 更新项目信息 | 高 | 单元/API |
| PROJ-004 | 删除项目 | 高 | 单元/API |
| PROJ-005 | 项目权限验证 | 中 | 集成 |
| PROJ-006 | 项目统计信息 | 低 | API |

### 章节管理模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| CHAP-001 | 创建章节 | 高 | 单元/API |
| CHAP-002 | 获取章节列表 | 高 | 单元/API |
| CHAP-003 | 更新章节内容 | 高 | 单元/API |
| CHAP-004 | 删除章节 | 高 | 单元/API |
| CHAP-005 | 章节版本管理 | 中 | 集成 |
| CHAP-006 | 章节确认发布 | 高 | 集成 |
| CHAP-007 | 章节排序 | 中 | API |

### 人物管理模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| CHAR-001 | 创建人物 | 高 | 单元/API |
| CHAR-002 | 获取人物列表 | 高 | 单元/API |
| CHAR-003 | 更新人物信息 | 高 | 单元/API |
| CHAR-004 | 删除人物 | 高 | 单元/API |
| CHAR-005 | 人物关系管理 | 中 | 集成 |
| CHAR-006 | 人物标签管理 | 低 | API |

### AI续写模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| AI-001 | AI续写请求 | 高 | 集成/API |
| AI-002 | AI改写请求 | 高 | 集成/API |
| AI-003 | AI润色请求 | 中 | 集成/API |
| AI-004 | AI扩写请求 | 中 | 集成/API |
| AI-005 | AI供应商切换 | 高 | 集成 |
| AI-006 | AI生成记录保存 | 高 | 集成 |
| AI-007 | AI生成草稿管理 | 高 | 集成 |

### 因果管理模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| CAUS-001 | 创建因果节点 | 高 | 单元/API |
| CAUS-002 | 创建因果边 | 高 | 单元/API |
| CAUS-003 | 更新因果权重 | 高 | 单元/API |
| CAUS-004 | 获取因果图谱 | 高 | API |
| CAUS-005 | 因果调整历史 | 中 | 集成 |
| CAUS-006 | 因果锁定功能 | 高 | 集成 |
| CAUS-007 | 因果建议生成 | 中 | 集成 |

### RAG模块
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| RAG-001 | 知识文档入库 | 高 | 集成 |
| RAG-002 | 知识分块处理 | 高 | 集成 |
| RAG-003 | 向量嵌入生成 | 高 | 集成 |
| RAG-004 | 知识检索 | 高 | 集成/API |
| RAG-005 | 检索结果排序 | 中 | 集成 |
| RAG-006 | RAG状态监控 | 低 | API |
| RAG-007 | 自动入库流程 | 高 | 端到端 |

### 系统功能
| 测试用例ID | 描述 | 优先级 | 测试类型 |
|------------|------|--------|----------|
| SYS-001 | 系统健康检查 | 高 | API |
| SYS-002 | 操作日志记录 | 中 | 集成 |
| SYS-003 | 错误处理 | 高 | 单元/集成 |
| SYS-004 | 性能监控 | 低 | 性能 |
| SYS-005 | 安全测试 | 高 | 安全 |

## 测试数据准备

### 数据库初始化
```sql
-- 测试数据库
CREATE DATABASE IF NOT EXISTS story_weaver_test
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 测试用户
INSERT INTO sys_user (username, password_hash, nickname, email, role_code, status) VALUES
('test_admin', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '测试管理员', 'admin@test.com', 'ADMIN', 'ACTIVE'),
('test_author', '$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx', '测试作者', 'author@test.com', 'AUTHOR', 'ACTIVE');

-- 测试项目
INSERT INTO novel_project (owner_id, name, slug, category, summary, status) VALUES
(1, '测试小说项目', 'test-novel', '玄幻', '这是一个测试小说项目', 'DRAFT');

-- 测试章节
INSERT INTO chapter (project_id, chapter_no, title, content_markdown, status) VALUES
(1, 1, '第一章：开端', '这是一个测试章节内容...', 'DRAFT');

-- 测试人物
INSERT INTO character_profile (project_id, name, gender, age_desc, is_main_character) VALUES
(1, '测试主角', '男', '18岁', 1);
```

### 测试配置文件
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/story_weaver_test
    username: test_user
    password: test_password
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    com.storyweaver: DEBUG
    org.springframework: INFO
```

## 测试执行指南

### 后端测试执行

#### 1. 单元测试
```bash
cd backend
mvn test -Dtest=*UnitTest
```

#### 2. 集成测试
```bash
cd backend
mvn test -Dtest=*IntegrationTest
```

#### 3. API测试
```bash
cd backend
mvn test -Dtest=*ApiTest
```

#### 4. 完整测试套件
```bash
cd backend
mvn clean test
```

### 前端测试执行

#### 1. 单元测试
```bash
cd front/vuetify-admin
npm run test:unit
```

#### 2. 组件测试
```bash
cd front/vuetify-admin
npm run test:component
```

#### 3. 端到端测试
```bash
cd front/vuetify-admin
npm run test:e2e
```

### 性能测试执行

#### 1. API性能测试
```bash
# 使用JMeter
jmeter -n -t tests/performance/api-load-test.jmx -l results.jtl
```

#### 2. 数据库性能测试
```bash
# 使用sysbench
sysbench oltp_read_write --db-driver=mysql prepare
sysbench oltp_read_write --db-driver=mysql run
```

## 测试覆盖率要求

### 后端覆盖率目标
- 行覆盖率: ≥ 80%
- 分支覆盖率: ≥ 70%
- 方法覆盖率: ≥ 85%

### 前端覆盖率目标
- 行覆盖率: ≥ 75%
- 分支覆盖率: ≥ 65%
- 函数覆盖率: ≥ 80%

### 集成测试覆盖率
- API端点覆盖率: 100%
- 核心业务流程覆盖率: 100%

## 测试报告

### 测试报告生成
```bash
# 后端测试报告
cd backend
mvn surefire-report:report
mvn jacoco:report

# 前端测试报告
cd front/vuetify-admin
npm run test:coverage
```

### 报告位置
- 单元测试报告: `backend/target/site/surefire-report.html`
- 代码覆盖率报告: `backend/target/site/jacoco/index.html`
- 前端测试报告: `front/vuetify-admin/coverage/index.html`

## 持续集成

### GitHub Actions配置
```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: root
          MYSQL_DATABASE: story_weaver_test
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=3
      
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '20'
    
    - name: Cache Maven dependencies
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Cache Node dependencies
      uses: actions/cache@v3
      with:
        path: front/vuetify-admin/node_modules
        key: ${{ runner.os }}-node-${{ hashFiles('front/vuetify-admin/package-lock.json') }}
        restore-keys: ${{ runner.os }}-node
    
    - name: Build and test backend
      run: |
        cd backend
        mvn clean compile test
    
    - name: Build and test frontend
      run: |
        cd front/vuetify-admin
        npm ci
        npm run build
        npm run test:unit
    
    - name: Upload test results
      uses: actions/upload-artifact@v3
      if: always()
      with:
        name: test-results
        path: |
          backend/target/surefire-reports/
          backend/target/site/jacoco/
          front/vuetify-admin/coverage/
```

## 故障排除

### 常见测试问题

#### 1. 数据库连接失败
```bash
# 检查MySQL服务
sudo systemctl status mysql

# 检查连接配置
mysql -u root -p -e "SHOW DATABASES;"
```

#### 2. 测试数据污染
```bash
# 清理测试数据库
mysql -u root -p -e "DROP DATABASE IF EXISTS story_weaver_test;"
mysql -u root -p -e "CREATE DATABASE story_weaver_test;"
```

#### 3. 端口冲突
```bash
# 检查端口占用
netstat -tulpn | grep :8080
netstat -tulpn | grep :5173

# 释放端口
kill -9 $(lsof -t -i:8080)
```

#### 4. 依赖问题
```bash
# 清理并重新安装
cd backend
mvn clean install -U

cd front/vuetify-admin
rm -rf node_modules package-lock.json
npm install
```

## 测试验收标准

### 必须通过的测试
1. 所有单元测试通过率 ≥ 95%
2. 所有集成测试通过率 ≥ 90%
3. 核心API测试通过率 100%
4. 安全测试无高危漏洞
5. 性能测试满足基准要求

### 发布标准
1. 测试覆盖率达标
2. 无阻塞性缺陷
3. 所有回归测试通过
4. 性能测试结果符合预期
5. 安全扫描通过

## 附录

### A. 测试工具版本
- JUnit: 5.10+
- Mockito: 5.7+
- Spring Boot Test: 3.2+
- TestContainers: 1.19+
- RestAssured: 5.4+
- Jacoco: 0.8.11+
- Jest: 29.7+
- Vue Test Utils: 2.4+
- Cypress: 13.6+

### B. 测试环境变量
```bash
export TEST_DB_HOST=localhost
export TEST_DB_PORT=3306
export TEST_DB_NAME=story_weaver_test
export TEST_DB_USER=test_user
export TEST_DB_PASSWORD=test_password
export TEST_REDIS_HOST=localhost
export TEST_REDIS_PORT=6379
export TEST_API_BASE_URL=http://localhost:8080/api
```

### C. 参考资料
1. [Spring Boot Testing Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
2. [Vue Test Utils Documentation](https://test-utils.vuejs.org/)
3. [Jest Documentation](https://jestjs.io/docs/getting-started)
4. [Cypress Documentation](https://docs.cypress.io/)
5. [JMeter User Manual](https://jmeter.apache.org/usermanual/index.html)
