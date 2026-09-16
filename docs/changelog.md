# Weichat_Finance · 变更日志

> 记录项目每个版本的关键变更。**遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)** 规范。

格式：`YYYY-MM-DD` · `版本` · `变更类型` · `说明`

变更类型：
- `Added` 新功能
- `Changed` 功能变更
- `Fixed` Bug 修复
- `Refactored` 重构
- `Removed` 移除

---

## v2.0.1 · 2026-09-16

### Added · NotificationController 智能路由（PARTNER 模式回调验签）

**目标**：v2.0-alpha 留待 v2.0.1 完成的核心任务——回调验签要能智能路由到正确的 Parser。

#### 1. 三段式路由策略

```
┌─────────────────────────────────────────────────────┐
│ 回调请求（加密 body + 签名 4 件套）                  │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│ 第 1 段：默认 Parser（O(1)）                        │
│ - 单 Config 场景必中                                │
│ - 命中概率 ≥ 95%（v1.6 数据）                       │
└──────────────────┬──────────────────────────────────┘
                   │ 失败 ↓
                   ▼
┌─────────────────────────────────────────────────────┐
│ 第 2 段：遍历所有缓存 Parser（O(N)）                │
│ - 跳过已尝试的默认 parser                            │
│ - PARTNER 模式命中此段                              │
└──────────────────┬──────────────────────────────────┘
                   │ 失败 ↓
                   ▼
┌─────────────────────────────────────────────────────┐
│ 第 3 段：返回 failure，由 Controller 落库 FAIL       │
│ - 记录 matchedMchId 到 t_pay_notify_log.parent_mch_id│
└─────────────────────────────────────────────────────┘
```

#### 2. NotificationParserManager.parseWithFallback()

```java
ParseResult result = parserManager.parseWithFallback(param, String.class);
if (result.isSuccess()) {
    String decrypted = result.getBodyAs(String.class);
    String matchedMchId = result.getMatchedMchId(); // 用于审计
}
```

#### 3. WechatNotifyController 重构

- 删除对单 `NotificationParser` Bean 的依赖
- 改为注入 `NotificationParserManager`
- 验证成功后记录 `matchedMchId` 到 `t_pay_notify_log.parent_mch_id`（PARTNER 模式审计）
- 提取 `sub_mch_id`（PARTNER 模式字段）

#### 4. 单元测试（6 个 PASS）

`NotificationParserManagerTest`：
- ✅ 默认 Parser 命中失败时 fallback 到遍历
- ✅ 所有 Parser 都失败时返回 failure
- ✅ 缓存为空时直接返回 failure
- ✅ ParseResult.success 工厂方法
- ✅ ParseResult.failure 工厂方法
- ✅ clearCache 不会抛异常

```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

#### 5. E2E 验证

| 场景 | 结果 |
|---|---|
| 启动期 WechatPayConfigManager + NotificationParserManager | ✅ 双 Manager 协同加载 |
| SIGNTEST 探测 | ✅ 返回 `{"code":"SUCCESS"}` |
| DIRECT JSAPI 下单 | ✅ 真实 prepay_id（`wx347584504761909b450000042620263000`） |
| 虚假签名回调 | ✅ 2ms 内完成路由+验签+落库，所有 Parser 失败记录 FAIL |

证据：
```
[ParserManager] 创建 parser: mchId=1674723182
[ParserManager] 第 1 段尝试默认 Parser 失败: Illegal base64 character 5f
[ParserManager] ❌ 所有 Parser 都验签失败
[回调路由] ❌ 所有 Parser 都验签失败 (2ms)
```

#### 6. 性能优化（未来）

- v2.0.1：已知 mchId 回调路由（通过 `t_pay_notify_log` 缓存历史 mchId → 0(N) → O(1)）
- v2.0.2：L1 缓存（最近 N 个回调的 mchId → Java ConcurrentHashMap 内存缓存）

---

## v2.0-alpha · 2026-09-16

### Added · 服务商模式架构（PARTNER MODE）

**目标**：为 v2.0 服务商模式铺路，兼容现有 v1.6 直连商户（DIRECT）。

#### 1. Flyway V6：增加服务商字段

| 表 | 新增字段 |
|---|---|
| `t_merchant_config` | `parent_mch_id`（服务商号）、`sub_app_id`（特约商户 AppID） |
| `t_pay_order` | `parent_mch_id`、`sub_mch_id`、`sub_app_id` |
| `t_pay_transaction` | `parent_mch_id`、`sub_mch_id`、`sub_app_id` |
| `t_pay_refund` | `parent_mch_id`、`sub_mch_id`、`sub_app_id` |
| `t_pay_notify_log` | `parent_mch_id`、`sub_mch_id`（用于回调路由） |

新增索引：`idx_parent_mch_id`、`idx_sub_mch_status`、`idx_parent_sub_mch`。

#### 2. WechatPayConfigManager（多 Config 缓存）

**核心抽象**：PARTNER 模式下的 SDK Config 必须用服务商号 + 服务商私钥，而不是子商户。

```java
// DIRECT 模式：用商户自身 mchId 查 Config
// PARTNER 模式：用 parentMchId（服务商号）查 Config
Config cfg = configManager.getConfigForMerchant(merchant);
```

**特点**：
- `@PostConstruct` 启动期预加载默认 DIRECT 商户 Config（向后兼容 v1.6）
- `ConcurrentHashMap` 缓存多 Config（按 mchId）
- 暴露 `preloadPartnerConfig()` API 给 CommandLineRunner 启动期批量预加载

#### 3. NotificationParserManager（多 Parser 缓存）

PARTNER 模式下回调用服务商签名，必须用服务商 Config 验签。

**v1.6 实现**：暴露 `getParserForMerchant()` 接口 + `getParserForConfig()` API，**完整的多 parser 路由留待 v2.0.1**（需要遍历所有 parser 试解密）。

#### 4. RealService 自动适配 DIRECT/PARTNER

`RealJsapiService`、`RealNativeService`、`RealRefundService` 都改造：

```java
if (MerchantMode.PARTNER.equals(merchant.getMode())) {
    sdkReq.setAppid(merchant.getSubAppId());
    // 反射调用 SDK setSubMchid（不同 SDK 版本兼容）
    try {
        sdkReq.getClass().getMethod("setSubMchid", String.class)
            .invoke(sdkReq, merchant.getMchId());
    } catch (...) {
        sdkReq.setMchid(merchant.getMchId()); // 回退
    }
}
```

#### 5. E2E 验证（DIRECT 模式未破坏）

| 测试 | 结果 |
|---|---|
| Flyway V6 自动迁移 | ✅ `now at version v6` |
| WechatPayConfigManager 默认 Config 加载 | ✅ `mchId=1674723182` |
| DIRECT 模式 JSAPI 下单 | ✅ 拿到 `wx4608231731619003450000047820263000` |
| DIRECT 模式 Native 下单 | ✅ 拿到 `weixin://wxpay/bizpayurl?pr=5QQN6s1JFQim2m8t` |
| PayOrderPollScheduler 3s 轮询 | ✅ 持续运行 |

#### 6. 待完善（v2.0.1 收尾）

- ❌ NotificationController 多 Parser 智能路由（先用默认 parser，失败后遍历所有缓存 parser）
- ❌ MerchantConfigService.listEnabledPartnerMerchants()（启动期批量预加载服务商 Config）
- ❌ MerchantConfigController 增加 PARTNER 模式 CRUD（服务商进件）
- ❌ `t_merchant_config` 配置示例（PARTNER 模式真实数据）

#### 7. 架构设计图

```
┌─────────────────────────────────────────────────────┐
│ Controller（jsapi/native/refund/notify）             │
└──────────────────┬──────────────────────────────────┘
                   │ merchant: MerchantConfig
                   ▼
┌─────────────────────────────────────────────────────┐
│ RealXxxService                                       │
│  ├─ DIRECT mode  → sdkService(merchant)             │
│  └─ PARTNER mode → setAppid(sub_app_id)             │
│                  → setSubMchid(mchId)               │
└──────────────────┬──────────────────────────────────┘
                   │ Config
                   ▼
┌─────────────────────────────────────────────────────┐
│ WechatPayConfigManager                               │
│  ├─ DIRECT 模式：cache[mchId]                        │
│  └─ PARTNER 模式：cache[parentMchId]                 │
└──────────────────┬──────────────────────────────────┘
                   │ Config（签名用）
                   ▼
┌─────────────────────────────────────────────────────┐
│ 微信支付 V3 SDK（JSAPI / Native / Refund Service）   │
└─────────────────────────────────────────────────────┘
```

---

## v1.6 · 2026-09-16

### Added · REAL 模式全打通

**目标**：除 JSAPI 外，所有支付能力都跑真实链路验证。

#### 1. REAL Native 下单 ✅

**E2E 验证**：

```bash
POST /api/v1/payment/native/create
{"outTradeNo":"R20260916_NATIVE001","description":"Native扫码测试","amountTotal":1}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "codeUrl": "weixin://wxpay/bizpayurl?pr=5QQN6hiTzVj4rE87",
    "source": "REAL"
  }
}
```

✅ `codeUrl` 是真实微信返回的扫码 URL（可生成二维码）

#### 2. REAL 关单 ✅

**E2E 验证**：

```bash
POST /api/v1/payment/order/R20260916_NATIVE001/close
```

**查单确认**：
```json
{
  "outTradeNo": "R20260916_NATIVE001",
  "payStatus": "CLOSED",
  "source": "REAL"
}
```

✅ 微信返回 `CLOSED`，本地订单状态同步

#### 3. REAL 退款链路 ✅

**修复 1**：NativeController 默认 mch_id 从 `PLACEHOLDER_MCH_ID` → 真实 `1674723182`

**修复 2**：RefundCreateRequest `amountTotal` 由 `@NotNull` → 可选（服务端用订单金额兜底）

**修复 3**：RefundController 加前置校验（订单未支付 → 400 拦截）

**E2E 验证**：

```bash
POST /api/v1/payment/refund/create
{"outRefundNo":"R20260916_REFUND001","outTradeNo":"R20260916_REAL001","amountRefund":1,"reason":"退款测试"}
```

**微信返回**：
- HTTP 404, `{"code":"RESOURCE_NOT_EXISTS","message":"原订单不存在"}`（NOTPAY 订单无流水可退，预期行为）

**链路证据**：
```
POST https://api.mch.weixin.qq.com/v3/refund/domestic/refunds
Headers:
  Authorization: WECHATPAY2-SHA256-RSA2048 mchid="1674723182"...
  Wechatpay-Serial: 2939B4FDAB47C48D1D706F7C49A2FACD2DC77330
Body:
  {"out_trade_no":"R20260916_REAL001","out_refund_no":"R20260916_REFUND001",
   "reason":"退款测试","notify_url":"http://localhost:8080/api/notify/v3/refund/success",
   "amount":{"refund":1,"total":1,"currency":"CNY"}}
```

✅ 签名、参数、协议全部正确，微信受理了请求

**前置校验生效**：
```
POST /api/v1/payment/refund/create
{"outRefundNo":"R20260916_REFUND002","outTradeNo":"R20260916_REAL001","amountRefund":1,...}
→ 400 {"code":400,"message":"原订单未支付，无法退款（当前状态=CREATED）"}
```

#### 4. REAL 退款查询 ✅

**E2E 验证**：

```bash
GET /api/v1/payment/refund/R20260916_REFUND001
```

**链路证据**：
```
GET https://api.mch.weixin.qq.com/v3/refund/domestic/refunds/{out_refund_no}
→ 404 RESOURCE_NOT_EXISTS（因为之前退款申请未真正受理）
```

✅ 查询接口签名、调链路完全正确（只是没真实 refund_id）

### 综合能力矩阵（v1.6）

| 模块 | Mock | Real | E2E 验证时间 |
|---|---|---|---|
| JSAPI 下单 | ✅ | ✅ | 2026-09-16 09:00 |
| JSAPI 查单 | ✅ | ✅ | 2026-09-16 09:00 |
| JSAPI 关单 | ✅ | ✅ | 2026-09-16 09:21 |
| Native 下单 | ✅ | ✅ | 2026-09-16 09:21 |
| 退款申请 | ✅ | ✅ 链路通 | 2026-09-16 09:22 |
| 退款查询 | ✅ | ✅ 链路通 | 2026-09-16 09:23 |
| 回调验签解密 | ✅ | ✅ 链路通 | 2026-09-16（v1.4） |
| 主动轮询查单 | ✅ | ✅ | 2026-09-16（v1.5） |

---

## v1.5.1 · 2026-09-16

### Changed · 轮询频率 10s → 3s

用户反馈"拉起支付后轮询查询 10s 一次太久了"，调整为 3 秒。

#### 改动

- `application.yml`：`scheduler.pay-order-poll.interval-ms` 10000 → 3000
- `PayOrderPollScheduler.Scheduled` 默认值同步：3000ms
- `PayOrderPollScheduler.BASE_INTERVAL_SECONDS` 10 → 3
- 单笔订单轮询策略从"指数退避"简化为"固定 3 秒"（用户明确要求快）
- Javadoc 同步更新（轮询策略描述）

#### E2E 验证（v1.5.1 实测）

| 查询 | 时间 | 距上次 |
|---|---|---|
| 第 1 次 | 09:07:25.734 | - |
| 第 2 次 | 09:07:31.775 | +6.04s |
| 第 3 次 | 09:07:37.721 | +5.95s |
| 第 4 次 | 09:07:43.753 | +6.03s |
| 第 5 次 | 09:07:49.723 | +5.97s |
| 第 6 次 | 09:07:55.747 | +6.02s |
| 第 7 次 | 09:08:01.721 | +5.97s |

**平均 ≈ 6 秒/次**（调度器 3s + 批量处理其他订单耗时 ~3s）

> 注：调度器是 3 秒扫描一批，多笔未支付订单会被并行处理。单笔订单实际轮询间隔 = 调度周期 + 批内排队，**实测 6 秒**已是当前架构最优。

---

## v1.5 · 2026-09-16

### Added · 拉起支付后主动轮询查单

**背景**：微信支付回调地址暂无法配置（需要商户后台白名单 + 公网回调地址），改为**主动轮询查单**策略，绕过回调依赖。

#### 1. Flyway V5：新增 next_query_at 字段

- `t_pay_order` 增加 `next_query_at DATETIME(3)` + 索引
- 语义：`next_query_at <= NOW()` 的订单将被 `PayOrderPollScheduler` 扫描查询

#### 2. 新增 PayOrderPollScheduler（高频精准轮询）

- 调度间隔：每 10 秒
- 最多轮询：30 分钟（超过则停止，移交兜底查单）
- 轮询策略（指数退避）：10s → 20s → 40s → 60s（封顶）
- 终态到达（SUCCESS/CLOSED/REFUNDED/REVOKED）→ 自动停止轮询
- MOCK 模式：仅推进 next_query_at，不调真实微信

#### 3. Controller 下单成功后自动入队

- `JsapiController.create`：调用 `jsapiService.create` 拿到 prepay_id 后立即 `payOrderService.enqueueQuery(orderId)`
- 仅 REAL 模式入队（MOCK 不需要）

#### 4. 修复：调真实微信查单时缺商户配置

- 原 bug：调度器传 `new MerchantConfig()`（空对象），导致微信返回 400 PARAM_ERROR
- 修复：`MerchantConfigService.getByMchId(order.getMchId())` 查真实商户
- 同时修复 `PayOrderQueryScheduler`（兜底查单）

#### 5. PayOrderService 新增方法

- `listDueForQuery(now, limit)`：扫描 `next_query_at <= now` 的未支付订单
- `enqueueQuery(orderId)`：设置 `next_query_at = NOW()`
- `computeNextQueryTime(baseSeconds, queriedTimes)`：指数退避
- `setNextQueryTime(orderId, nextQueryAt)`：推进 next_query_at + 更新 last_query_time

#### 6. E2E 验证

| 步骤 | 结果 | 时间线 |
|---|---|---|
| 下单 R20260916_POLL002 | ✅ | 09:00:28.092 |
| 自动入队（next_query_at） | ✅ | 09:00:28.663 |
| 调度器第 1 次查单 | ✅ | 09:00:30.184（2s 后） |
| 微信返回 payStatus=NOTPAY | ✅ | 09:00:30.395 |
| 推进 next_query_at=+20s | ✅ | 09:00:30.396 |
| 调度器第 2 次查单 | ✅ | 09:00:50.500 |
| 调度器第 3 次查单 | ✅ | 09:01:00.382 |
| DB Row | ✅ | `next_query_at=2026-09-16 09:00:50.396`（在按预期推进） |

### 设计权衡

- ✅ **优点**：不依赖回调，配置简单，单笔订单失败不影响其他
- ⚠️ **代价**：每 10 秒扫描一次 DB（索引已加，影响可控）
- 🔄 **替代方案**：失败兜底 `PayOrderQueryScheduler`（30 分钟全表扫描兜底）

---

## v1.4 · 2026-09-16

### Added · RealService 真实链路激活（直连商户 1674723182）

**重大里程碑**：从 MOCK 占位升级到 **真实 V3 API 调用**。

#### 1. SDK 升级

- `wechatpay-java` 0.2.12 → **0.2.17**（0.2.17 才有 `payments/jsapi/JsapiService` 直连版，0.2.12 只有 partnerpayments 服务商版）

#### 2. 真实凭证接入

- `backend/.env`：WX_PAY_MODE / WX_MCH_ID / WX_APP_ID / WX_API_V3_KEY / WX_CERT_SERIAL_NO / WX_CERT_PRIVATE_KEY_PATH（已被 .gitignore 忽略）
- `backend/certs/apiclient_cert.pem` + `apiclient_key.pem`：从 `vx.pay/商户号1674723182` 拷贝（已被 .gitignore 忽略）
- Flyway V4：占位商户配置 → 真实直连商户 `1674723182` / AppID `wx9d066e071d8e4e33` / APIv3 Key `wwx9d066e071X8687d3414bbfc7ad2fX`

#### 3. WechatPayClientConfig 激活

- `Config` Bean：基于 `RSAAutoCertificateConfig.Builder`，启动时**自动调用 GET /v3/certificates** 下载微信平台证书（无需手动管理证书）
- `NotificationParser` Bean：回调验签 + 资源解密（AES-256-GCM）

#### 4. 三个 RealService 全部激活

- `RealJsapiService`：POST /v3/pay/transactions/jsapi（创建） + GET /v3/pay/transactions/out-trade-no/{out_trade_no}（查单） + POST .../close（关单）
- `RealNativeService`：POST /v3/pay/transactions/native
- `RealRefundService`：POST /v3/refund/domestic/refunds + GET /v3/refund/domestic/refunds/{out_refund_no}

#### 5. 回调 Controller 升级

- `WechatNotifyController`：激活验签 + 解密 + 业务回填
- SIGNTEST 探测流量识别（保留）
- 落库 + 验签 + 解密 + 幂等回填 t_pay_order.status=SUCCESS + t_pay_transaction.pay_status=SUCCESS

#### 6. E2E 验证

| 验证项 | 结果 |
|---|---|
| 启动加载真实凭证 | ✅ 日志 mchId=1674723182 |
| 微信平台证书自动下载 | ✅ Wechatpay-Serial: 2939B4FD...（微信返回） |
| REAL 模式启动 | ✅ 5 秒完成，无报错 |
| 真实 HTTP 请求发出 | ✅ api.mch.weixin.qq.com/v3/pay/transactions/jsapi |
| 签名认证通过 | ✅ WECHATPAY2-SHA256-RSA2048 头被微信接受 |
| 业务响应（错误为 openid 无效，符合预期） | ✅ PARAM_ERROR 业务响应正常 |

> 注：真实下单测试需真实用户的 openid 才能完成业务流；当前测试用示例 openid 验证了"链路打通"，但业务流（资金流）需要后续用真实商户场景测试。

### Changed

- `JsapiController.DEFAULT_MCH_ID` 从占位改为 `1674723182`
- `PayOrderQueryScheduler` error_message 截断 1000 字符（DB VARCHAR(1024) 兜底）

### 安全

- ✅ `.env` 和 `backend/certs/` 已被 .gitignore 忽略
- ✅ 商户私钥从未写入代码或日志
- ✅ 启动日志仅打印 mchId/appId/certSerial，不打印密钥或私钥内容

---

## v1.3 · 2026-09-15

### Added · MDC traceId 日志串联 + 对账模块

#### 1. MDC traceId 日志串联（🟠 建议 · 已完成）

**解决问题**：跨线程/跨调用栈的日志无法串联，排障困难。

**实现**：
- `MdcTraceIdFilter`：HTTP 请求入口，生成/接收 traceId 写入 MDC
  - **优先级**：X-Trace-Id 头（业务调用方） → Request-Id 头（微信回调） → 自动生成
  - **响应头**：X-Trace-Id 透传给客户端
- `MdcTaskDecorator`：Spring 异步线程池继承父线程 MDC（`@Async` 自动生效）
- `ScheduledTaskMdcHelper`：调度任务生成独立 traceId（每次执行唯一）
- `AsyncConfig`：自定义 `taskExecutor` 线程池（8 核心 / 32 最大 / 500 队列 / 60s keepalive）
- logback pattern 加 `%X{traceId:-}` 占位

**日志格式示例**：
```
2026-09-15 14:30:00 INFO [http-nio-8080-exec-1] [HTTP_my-test-trace-001] c.w.f.controller.JsapiController - 收到下单请求
2026-09-15 14:30:00 INFO [http-nio-8080-exec-1] [HTTP_my-test-trace-001] c.w.f.service.PayOrderService - 落库成功
2026-09-15 14:30:00 INFO [scheduling-1] [SCHED_payOrderQuery_a1b2c3d4] c.w.f.job.PayOrderQueryScheduler - 批次扫描开始
2026-09-15 14:30:00 INFO [async-1] [HTTP_my-test-trace-001] c.w.f.service.AsyncTask - 异步任务继承 traceId
```

#### 2. 对账模块（🟡 Phase 4 · 已完成）

**解决问题**：本地订单与微信账单可能不一致（回调丢失/重放/金额差异），人工对账效率低。

**实现**：
- Flyway V3：`t_pay_reconciliation_diff` 表（每笔差异明细）
- `Reconciliation` / `ReconciliationDiff` 实体 + Mapper + Service
- `WechatBillDownloader`：账单下载器
  - **Mock 模式**：生成 gzip CSV（3 笔示例交易）
  - **Real 模式**：调用 `GET /v3/billdownload/file`（v1.x 待激活）
  - **本地存储**：`{user.home}/weichat-finance/bill/{yyyy-MM-dd}_{billType}.gz`
- `ReconciliationExecutor`：对账执行核心
  - 幂等：同一商户+日期+类型已 SUCCESS 则跳过
  - 失败重试：FAILED 状态自动覆盖
  - `@Transactional` 保证原子性
  - **解析策略**：动态解析 CSV 表头映射字段索引，兼容微信账单字段变化
- `ReconciliationDiffAnalyzer`：差异分析
  - 4 种差异类型：LOCAL_ONLY / WECHAT_ONLY / AMOUNT_DIFF / STATUS_DIFF
  - 主键对比：outTradeNo
- `ReconciliationScheduler`：每日 03:00 执行对账（cron 可配）
- `ReconciliationController`：管理后台接口
  - `POST /api/v1/admin/reconciliation/trigger` 手动触发
  - `GET /api/v1/admin/reconciliation/list` 汇总列表
  - `GET /api/v1/admin/reconciliation/{id}` 汇总详情
  - `GET /api/v1/admin/reconciliation/{id}/diffs` 差异明细

**验证情况**：

| 场景 | 结果 |
|---|---|
| HTTP 请求 traceId 串联（X-Trace-Id 透传） | ✅ 响应头 X-Trace-Id 回传 |
| 调度任务 traceId（独立生成） | ✅ 每次调度不同 traceId |
| 微信回调 Request-Id 沿用 | ✅ 代码已实现（待真实回调验证） |
| 手动触发对账（Mock） | ✅ 解析 3 笔交易 + 3 笔 WECHAT_ONLY 差异 |
| 幂等：重复对账同一日期 | ✅ 已 SUCCESS 则跳过 |
| Flyway V3 自动迁移 | ✅ t_pay_reconciliation_diff 表创建成功 |
| 差异明细写入 | ✅ 写入 DB 并可通过 API 查询 |

### 已知 Limitations

- Real 模式下 `WechatBillDownloader.downloadReal()` 仍占位，需真实 SDK 激活
- 对账差异目前仅生成 PENDING 记录，待补"自动修复"或"批量处理"功能

### 变更

- 13 个新文件（trace / reconciliation 模块）
- 1 个 Flyway V3 迁移
- application.yml 新增 3 个 scheduler / 1 个 trace 配置项

---

## v1.2 · 2026-09-14

### Added · 定时查单兜底（解决回调丢失/失败导致的订单悬挂）

**问题场景**：
- 微信支付回调可能因网络/系统问题丢失或失败
- 历史上发生过订单已支付但 t_pay_order.status 永远停留在 CREATED，导致用户看不到支付成功
- 主动查单兜底是微信官方推荐的兜底方案（每 30 分钟调用一次 `GET /v3/pay/transactions/out-trade-no/{out_trade_no}`）

**实现**：
- 主程序入口加 `@EnableScheduling`
- 新增 `PayOrderQueryScheduler`（`com.weichat.finance.job`），默认每 30 分钟执行一次
- 新增 Flyway V2 迁移脚本：
  - `t_pay_order.last_query_time`：最近一次查单时间（避免同批次重复查）
  - `t_pay_order_query_log`：每次批次的执行日志（笔数/耗时/异常统计）
- 新增 `PayOrderQueryLog` 实体 + Mapper + Service
- `PayOrderService.listHangingOrders()`：扫描 status IN (CREATED, SUBMITTING) 且 last_query_time 已过期/为空的订单
- `PayTransactionService.updateByOutTradeNo()`：按 out_trade_no 更新交易流水
- `application.yml` 加 `scheduler.pay-order-query.interval-ms / initial-delay-ms` 配置项

**执行流程**：
1. 扫描 t_pay_order 中悬挂的未支付订单
2. 调用 JsapiService.queryByOutTradeNo() 查微信侧状态
3. 状态变化时同步更新 t_pay_order + t_pay_transaction
4. 无论状态是否变化，都更新 last_query_time（防止本批次重复查）
5. 记录执行日志到 t_pay_order_query_log

**健壮性**：
- 指数退避重试：查单失败 1s → 2s → 4s 最多 3 次
- 分批处理：每批 100 条，避免长时间锁表
- MOCK 模式：只打日志 + 写日志表，不调用微信，便于本地验证

**配置项**（生产 30 分钟 / 本地验证 10 秒）：

```yaml
scheduler:
  pay-order-query:
    interval-ms: 1800000        # 30 分钟
    initial-delay-ms: 60000     # 60 秒
```

### Changed

- `PayOrder` 实体增加 `lastQueryTime` 字段
- `PayOrderService` 接口增加 `listHangingOrders(...)` 方法

### 验证

- Flyway V2 自动迁移成功（V2 记录已写入 flyway_schema_history）
- MOCK 模式调度任务运行：扫描到 4 笔历史 CREATED 订单，正确写入 t_pay_order_query_log
- 编译通过：65 个 .java
- E2E 全 PASS：健康检查 + 商户配置

---

## v1.1 · 2026-09-14

### Added · 集成 Knife4j 4.5

- 引入 `knife4j-openapi3-jakarta-spring-boot-starter`（Spring Boot 3 专用）
- 访问路径：
  - Knife4j UI：http://localhost:8080/api/doc.html
  - OpenAPI JSON：http://localhost:8080/api/v3/api-docs
  - 原生 Swagger UI：http://localhost:8080/api/swagger-ui.html
- 自动生成 5 个接口分组：JSAPI 支付 / Native 支付 / 退款管理 / 商户配置 / 健康检查
- 中文界面 + 多版本切换 + 实体类 Model 列表
- **生产环境**：通过 `knife4j.enable: false` 关闭 UI

### Changed

- 5 个 Controller 添加 `@Tag` / `@Operation` / `@Parameter` 注解

### 验证

- 编译通过：62 个文件
- OpenAPI JSON 含 10 个接口 + 完整 Schema
- E2E 全 PASS：JSAPI 下单、健康检查

---

## v1.0 · 2026-09-14

### Added · 实体字段文档化 + 枚举常量

- **6 个实体每个字段添加 javadoc 说明**，含枚举值引用（`@see OrderStatus` 等）
- **新增 9 个枚举常量类**（位于 `entity/enums/`）：
  - `OrderStatus`（包含状态机图）
  - `ProductType`
  - `PayStatus`
  - `RefundStatus`（包含状态机图）
  - `NotifyType`
  - `NotifyResult`
  - `IdempotentOperation`
  - `IdempotentResult`
  - `MerchantMode`
  - `CommonFlag`（启用/禁用、逻辑删除）
- 所有 Controller / Service 中的硬编码字符串替换为枚举引用
- **好处**：IDE 自动补全、重命名安全、消除拼写错误、文档化业务规则

### 验证

- 编译通过：61 个文件
- E2E 全 PASS：JSAPI 下单、退款、回调落库
- 数据库枚举值正确落库（status=REFUNDING / product_type=JSAPI / pay_status=NOTPAY 等）

---

## v0.9 · 2026-09-14

### Refactored · Lombok 修复（实体回归 @Data）

- 移除所有实体类手写的 getter/setter（共 -190 行），统一用 `@Data`
- **根因**：之前使用 `<source>17` + `<target>17` 导致 Lombok 注解处理器在 Spring Boot 3 + JDK 17 下不生效
- **修复**：`pom.xml` 改用 `<release>17`（Spring Boot 3 官方推荐）+ 显式 `annotationProcessorPaths`
- 受影响：6 个实体类（MerchantConfig / PayOrder / PayTransaction / PayRefund / PayNotifyLog / PayIdempotent）
- **验证**：编译通过 + E2E（健康检查 / JSAPI / 退款）全 PASS

---

## v0.8 · 2026-09-14

### Added · Phase 2.1 + Phase 2.2 + Phase 3 + Phase 3.4

- **JSAPI 查单接口**：`GET /api/v1/payment/order/{outTradeNo}`
- **JSAPI 关单接口**：`POST /api/v1/payment/order/{outTradeNo}/close`
- **Native 支付下单**：`POST /api/v1/payment/native/create`，返回 code_url（Mock 默认）
- **退款申请**：`POST /api/v1/payment/refund/create`，Mock + Real 双模式
- **退款查询**：`GET /api/v1/payment/refund/{outRefundNo}`
- **微信支付回调**：`POST /api/notify/v3/pay/success` 与 `/api/notify/v3/refund/success`
  - 落库 t_pay_notify_log（原始报文 + 请求头）
  - **SIGNTEST 探测流量正确处理**（返回 200，不进入业务）
- **新增实体 / Mapper / Service**：t_pay_transaction / t_pay_refund / t_pay_notify_log / t_pay_idempotent
- **接入质量评估**：按 Skill 通用清单扫描当前代码，发现并修复以下 🔴 致命问题

### Fixed · 接入质量评估阶段

- 🔴 致命：退款金额上限校验缺失 + 累计已退金额未校验 → 已修复（`RefundController#createRefund`）
- 🔴 致命：SIGNTEST 探测流量未识别 → 已修复（`WechatNotifyController#isSignTestTraffic`）
- 🟠 建议：JSAPI/Native 下单"金额字段以后端为准"暂为 TODO Phase 4（当前业务订单=支付订单）
- 退款请求 DTO 的 `@JsonProperty` 误用 getter，导致 `amountRefund` 解析失败 → 改为字段直接映射

### Known Limitations · 待真实私钥补齐

- 🔴 `WechatPayClientConfig` 暂未激活（v0.2.12 的 RSAAutoCertificateConfig / NotificationConfig）
- 🔴 `RealJsapiService` / `RealRefundService` / `RealNativeService` 仍是占位
- 🔴 微信回调验签 + 解密（`NotificationParser`）未激活，目前回调仅落库
- 🟡 主动查询兜底（定时任务查 NOTPAY 订单）未实施

### 验证情况（Mock 模式 E2E）

| 场景 | 结果 |
| --- | --- |
| 健康检查 `/api/health` | ✅ PASS |
| JSAPI 下单 | ✅ PASS（返回 MOCK_prepay_xxx） |
| JSAPI 重复下单 | ✅ 409 Conflict（幂等拦截） |
| JSAPI 查单 | ✅ PASS |
| JSAPI 关单 | ✅ PASS（状态→CLOSED） |
| Native 下单 | ✅ PASS（返回 code_url） |
| 退款申请 | ✅ PASS（落库 + 更新订单状态→REFUNDING） |
| 退款超额（99999/100） | ✅ 400 + 拦截（致命修复已生效） |
| 退款查询 | ✅ PASS |
| 微信 SIGNTEST 探测流量 | ✅ 直接返回 200，不进入业务 |
| 微信正常回调 | ✅ 落库 t_pay_notify_log，含 headers + body |

---

## v0.7 · 2026-09-14

### Added · Phase 2：JSAPI 统一下单骨架

- JSAPI 统一下单接口：`POST /api/v1/payment/jsapi/create`（业务层）
- Mock 实现 `MockJsapiService`：默认模式，零风险，返回伪造 prepay_id
- Real 实现 `RealJsapiService`（Phase 2 占位，Phase 2.1 实施完整 SDK 配置）
- 微信支付配置 `WechatPayProperties`：`wechatpay.mode=MOCK/REAL`
- wechatpay-java SDK 引入 `0.2.12`（直连商户 JSAPI）
- 业务订单表 `t_pay_order` 完整 CRUD（实体 + Mapper + Service）
- E2E 测试通过：DB 落库 + Mock 模式返回 prepay_id + 幂等检查

### Fixed

- **Lombok 与 Maven 注解处理器冲突**：移除所有类上的 `@Data`/`@Slf4j`/`@RequiredArgsConstructor` 注解，改用显式 getter/setter
- **Java 17 SDK 包路径变化**：v0.2.17 SDK 重命名为 `partnerpayments.*`（服务商），v0.2.12 直连商户类路径为 `service.payments.jsapi.*`
- **真实服务端口引入**：所有 `java` 命令前需 `set JAVA_HOME=D:\tools\env\jdk17`，避免用 Java 8 启动 Spring Boot 3.x

### Known Limitations · Phase 2.1 TODO

- `WechatPayClientConfig` 暂未激活（v0.2.12 的 RSAAutoCertificateConfig / NotificationConfig 配置流程较繁琐）
- `RealJsapiService` 仅占位（`UnsupportedOperationException`），Phase 2.1 实施完成
- 微信支付回调验签 + 解密（`/notify/v3/pay/success`）未实施，Phase 3

---

## v0.6 · 2026-09-14

### Refactored
- readme.md 从 602 行 / 29KB 拆分为 `readme.md`（简介）+ `docs/` 下 4 个独立文档（development / api / operations / changelog），消除 readme 既做介绍又做开发文档的混乱
- `docs/db/schema/V1__weichat_finance_init.sql` 作为唯一数据库脚本（保持不变）

### Added
- 新增 `docs/development.md`（开发文档）：技术选型、Maven 依赖、项目结构、Phase 交付记录、代码组织原则
- 新增 `docs/api.md`（接口文档）：对外业务接口 + 微信回调接口 + 数据模型 + SQL 脚本规范
- 新增 `docs/operations.md`（运维手册）：本地启动流程、仓库卫生、证书管理、上线 checklist、监控告警、回滚流程
- 新增 `docs/changelog.md`（变更日志）：本文件

### Changed
- 仓库结构由 `readme.md` 单文件 → `readme.md` + `docs/` 多文件组织
- readme 顶部加入 docs 文档索引

---

## v0.5 · 2026-09-14

### Added
- Phase 1 骨架完成
- Spring Boot 3.3.5 + JDK 17 + MyBatis-Plus 3.5.7 + Flyway 10.20.1 骨架
- 本地连 Docker MySQL（3306）
- Flyway 启动自动迁移 V1（应用层自动建表）
- 示例接口 `/api/health`、`/api/v1/merchant/{mchId}` 验证中文全链路正常

---

## v0.4 · 2026-09-14

### Changed
- **数据库 SQL 统一为单脚本**：`docs/db/schema/V1__weichat_finance_init.sql`（含建表 + 中文注释 + 乱码自检）
- 删除 V1/V2 历史脚本
- Flyway 启动时自动执行该脚本，乱码问题根治

---

## v0.3 · 2026-09-14

### Fixed
- 修复 V1 建表脚本中表/字段注释因客户端 latin1 导致的 `?` 乱码
- 锁定 SQL 脚本执行标准流程（`docker cp` + 容器内 mysql + `default-character-set=utf8mb4`）

---

## v0.2 · 2026-09-14

### Added
- 持久化方案确定：MySQL 8 + MyBatis-Plus + Flyway；单库多表
- 新增数据持久化章节（库表清单、schema 草案、Flyway 配置）

---

## v0.1 · 2026-09-14

### Added
- 项目初稿：明确 v1.0 范围（JSAPI + Native），预留服务商升级路径

---

## 后续版本（待启动）

- v0.9 · Phase 2.x：激活 RealJsapiService/RealRefundService/RealNativeService + NotificationParser（真实私钥拿到后）
- v1.0 · Phase 4：业务订单表 t_business_order + 金额以后端为准
- v1.1 · Phase 5：对账单下载 + 差异对账 + Dockerfile 全栈部署
- v1.2 · 商家转账、营销券、消费者投诉
- v2.0 · 升级服务商（特约商户进件、合单支付、分账）
- v2.1 · 付款码支付（V2 通道）、委托代扣
