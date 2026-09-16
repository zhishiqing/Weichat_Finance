# 微信支付 Demo · 前后端联调

> 把上一版"酒店预订 Demo"**改造为微信支付收银台**，复用后端真实 API。

## 🎯 设计目标

- **业务匹配**：前端页面用微信支付收银台语义（不再虚构"酒店预订"）
- **真实联调**：10 个页面全部走真实后端 API
- **链路透明**：traceId 透传 + 响应原文展示

## 📂 页面清单（10 页闭环）

| # | 文件 | 路由 | 后端 API |
|---|---|---|---|
| 1 | `index.html` | `/` | `GET /health` + `GET /v1/merchant` |
| 2 | `booking.html` | `/booking.html?type=jsapi\|native` | `POST /v1/payment/jsapi/create` 或 `/native/create` + `GET /v1/merchant/{mchId}` |
| 3 | `payment.html` | `/payment.html?otn=xxx` | `GET /v1/payment/order/{otn}`（3 秒轮询） |
| 4 | `success.html` | `/success.html?otn=xxx` | `GET /v1/payment/order/{otn}` |
| 5 | `detail.html` | `/detail.html?otn=xxx` | `GET /v1/payment/order/{otn}` + `POST /close` + `POST /refund/create` |
| 6 | `orders.html` | `/orders.html` | `GET /v1/payment/order/{otn}`（本地缓存 + 实时校验） |
| 7 | `me.html` | `/me.html` | `GET /v1/merchant/{mchId}` |
| 8 | `login.html` | `/login.html` | **API 调试中心**（直接调用所有后端接口） |
| 9-10 | （合并到 1 + 7） | — | — |

## 🔧 关键技术点

### 1. CORS

后端 `CorsConfig.java` 放行 `http://localhost:*`（开发期）

```java
config.addAllowedOriginPattern("http://localhost:*");
config.addAllowedOriginPattern("http://127.0.0.1:*");
```

### 2. traceId 透传

前端每次请求生成 `fe-xxx` 12 位 ID → 通过 `X-Trace-Id` header → 后端 MDC → 日志串联

```js
const traceId = 'fe-' + Math.random().toString(36).slice(2, 14);
fetch(url, { headers: { 'X-Trace-Id': traceId, ... } })
```

后端日志格式：
```
[d{yyyy-MM-dd HH:mm:ss.SSS}] [level] [thread] [traceId] [className] - message
```

### 3. 统一响应 R<T>

```json
{ "code": 200, "message": "success", "data": {...} }
```

前端自动解包：
```js
if (data.code !== 200) throw error;  // 弹 toast
return data.data;                     // 业务数据
```

### 4. 轮询查单

支付页启动 3 秒间隔轮询 → 直到 `PAID / CLOSED / REFUNDED` → 自动跳转

```js
timer = setInterval(poll, 3000);
```

### 5. 状态映射

| 后端 status | 前端显示 |
|---|---|
| SUBMITTING | 提交中 |
| CREATED | 已创建 |
| NOTPAY | ⏰ 待支付 |
| PAID | ✅ 已支付 |
| SUCCESS | 🎉 已完成 |
| CLOSED | ✕ 已关闭 |
| REFUND | 💸 退款中 |
| REFUNDED | ✓ 已退款 |

## 🚀 联调步骤

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
# 默认端口 8080，context-path=/api
# 健康检查：http://localhost:8080/api/health
```

### 2. 启动前端

```bash
npx http-server design-preview/pay-demo -p 8091 -c-1
# → http://localhost:8091/index.html
```

### 3. 联调流程

1. **首页** → 自动调用 `GET /health`，绿色脉冲点 = 服务正常
2. **点击"JSAPI 下单"** → 跳到 `booking.html`，默认填好商户 + 0.01 元
3. **点"提交订单"** → 调 `POST /v1/payment/jsapi/create` → 跳支付页
4. **支付页** → 3 秒轮询 `GET /v1/payment/order/{otn}`，MOCK 模式永远返回 NOTPAY（演示用）
5. **点"查看详情"** → 看完整 traceId + 响应原文 + 时间线
6. **点"主动查单"** → 手动触发一次查询，看 traceId 变化

### 4. 调试技巧

**F12 Network 面板**：
- 每个请求头 `X-Trace-Id: fe-xxxxxx`
- 后端日志搜索 `fe-xxxxxx` 可看到完整链路

**Knife4j 调试**：<http://localhost:8080/api/doc.html>

**Postman**：参考 `postman/` 目录（旧版）

## 🧪 演示数据

- 默认商户：`1674723182`（已落库）
- 默认 AppID：`wx0000000000000000`
- 默认金额：¥0.01（1 分）

## ⚙️ 切换 REAL 模式

```bash
# 后端
export WX_PAY_MODE=REAL
export WX_MCH_ID=真实商户号
export WX_APP_ID=真实AppID
export WX_API_V3_KEY=真实32位key
export WX_CERT_SERIAL_NO=真实证书序列号
export WX_CERT_PRIVATE_KEY_PATH=certs/apiclient_key.pem
mvn spring-boot:run
```

REAL 模式下，调用会走真实微信 API（需要商户已开通 V3 接口）。

## 📁 目录结构

```
pay-demo/
├── index.html              # 1 收银台首页（健康检查 + 商户列表）
├── booking.html            # 2 创建订单（金额 + 商品 + openid）
├── payment.html            # 3 支付页（轮询查单 + 倒计时）
├── success.html            # 4 成功页（traceId 链路追踪）
├── detail.html             # 5 订单详情（时间线 + 关单 + 退款）
├── orders.html             # 6 交易记录（5 Tab）
├── me.html                 # 7 商户信息（API 端点列表）
├── login.html              # 8 API 调试中心
├── README.md               # 本文档
└── assets/
    ├── api.js              # API 客户端（fetch + R<T> + traceId）
    ├── styles.css          # 设计 tokens + 组件样式
    └── components.js       # Nav/Footer/Frap/Toast
```

## 🐛 已知问题

1. **MOCK 模式不模拟支付成功** → 详情页 status 永远 NOTPAY，需手动测试"关闭/退款"
2. **无二维码库** → Native 模式展示 code_url 文字，不生成真实二维码
3. **无微信 JSAPI SDK** → JSAPI 模式展示"调用步骤"，不真实拉起微信
4. **无后端日志** → 前端看不到后端处理细节，只能看到响应

## 🎨 设计延续

保留 Starbucks 设计系统：
- 四层绿色 + 暖奶油背景 + 50px pill 按钮 + Frap 浮动 CTA
- 详情页 + 成功页有绿色弹入对勾
- 调试中心用金色 dashed 边框区分"调试"语义
