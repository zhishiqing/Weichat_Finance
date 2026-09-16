# Hotel Booking · 星巴克风格前端 Demo

完整的酒店预订前端项目，10 个页面闭环。**无后端依赖**，使用 `localStorage` 持久化订单数据。

## 🎨 设计系统

延续 [Starbucks DESIGN.md](../../DESIGN.md) 风格：
- **四层绿色系统**（Starbucks / Accent / House / Uplift）
- **暖奶油背景** `#f2f0eb`（不是纯白）
- **50px 全 pill 按钮** + `scale(0.95)` 按下
- **Frap 浮动圆形 CTA**（56px）
- **Inter 字体** + `-0.01em` 紧凑跟踪

## 📂 页面清单（10 个完整页面闭环）

| # | 文件 | 路由 | 说明 |
|---|---|---|---|
| 1 | `index.html` | `/` | **首页**：Hero 搜索框 + 6 个热门目的地 + 6 个推荐酒店 |
| 2 | `list.html` | `/list.html?city=上海` | **列表页**：左侧筛选（价格/星级/设施）+ 5 排序 + 列表卡 |
| 3 | `detail.html` | `/detail.html?id=h001` | **酒店详情**：5 图集 + 房型选择 + 设施 + 评价 + 周边 + 底部浮动价格条 |
| 4 | `booking.html` | `/booking.html` | **预订表单**：6 步（日期/入住人/偏好/担保/增值/支付） |
| 5 | `payment.html` | `/payment.html?oid=xxx` | **支付页**：深绿倒计时（15 分钟）+ 4 种支付方式 + 订单摘要 |
| 6 | `success.html` | `/success.html?oid=xxx` | **成功页**：弹入对勾 + 优惠券赠送 + 完整订单信息 |
| 7 | `orders.html` | `/orders.html` | **订单中心**：5 Tab（全部/待支付/已支付/已完成/已取消）+ 4 操作按钮（去支付/取消/再次预订/删除） |
| 8 | `order-detail.html` | `/order-detail.html?oid=xxx` | **订单详情**：状态 banner + 4 步时间线 + 取消/退款 |
| 9 | `me.html` | `/me.html` | **个人中心**：金卡会员 + 4 数据卡 + 8 常用功能 + 3 设置项 + 退出登录 |
| 10 | `login.html` | `/login.html` | **登录页**：密码/短信双 Tab + 微信/QQ/Apple 三方登录 + 倒计时 |

## 📊 页面闭环图

```
                         ┌─────────┐
                         │ 登录    │
                         └────┬────┘
                              ↓
                         ┌─────────┐
                         │ 首页    │ ← 热门城市 / 推荐酒店
                         └────┬────┘
                              ↓
                         ┌─────────┐
                         │ 列表页  │ ← 筛选 / 排序
                         └────┬────┘
                              ↓
                         ┌─────────┐
                         │ 详情页  │ ← 图集 / 房型 / 评价
                         └────┬────┘
                              ↓ 选房型
                         ┌─────────┐
                         │ 预订页  │ ← 入住人 / 偏好 / 担保
                         └────┬────┘
                              ↓ 提交
                         ┌─────────┐
                         │ 支付页  │ ← 15分钟倒计时 / 4种方式
                         └────┬────┘
                              ↓ 支付成功
                         ┌─────────┐
                         │ 成功页  │ ← 优惠券 / 详情
                         └────┬────┘
                              ↓
                         ┌─────────┐
                         │ 订单中心│ ← 5 Tab / 多操作
                         └────┬────┘
                              ↓ 点订单
                         ┌─────────┐
                         │订单详情 │ ← 时间线 / 取消退款
                         └────┬────┘
                              ↓
                         ┌─────────┐
                         │ 我的    │ ← 会员 / 功能 / 设置
                         └─────────┘
```

## 🔧 技术栈

- **纯 HTML + CSS + JavaScript**（无框架依赖）
- **localStorage** 存储订单（订单自动持久化）
- **sessionStorage** 存储预订 cart
- **Google Fonts** 加载 Inter
- **Unsplash** 加载酒店图片（带 onerror 降级）

## 🚀 本地预览

```bash
# 方式 1：直接打开
start hotel/index.html    # Windows
open hotel/index.html     # macOS

# 方式 2：起本地服务
npx http-server hotel -p 8090 -c-1
# → http://localhost:8090/index.html
```

## 🧪 测试流程建议

1. **打开** `index.html` → 看 Hero 搜索框 + 6 城市 + 6 推荐酒店
2. **点任意酒店** → 进入详情页 → 选房型 → 底部浮动价格条出现
3. **点预订** → 跳转登录页（未登录）→ 演示账号一键登录
4. **填入住人** → 提交订单 → 跳转支付页（15 分钟倒计时）
5. **点确认支付** → 2 秒后跳转成功页（弹入对勾 + 优惠券）
6. **点返回订单中心** → 看到刚下的订单（已支付 Tab）
7. **点订单** → 查看详情（时间线）→ 申请退款 → 状态变 "已退款"
8. **到我的** → 看见订单统计、会员等级、功能入口

## 💾 数据持久化

| Key | 存储 | 用途 |
|---|---|---|
| `hotel_orders` | localStorage | 订单列表 |
| `hotel_user` | localStorage | 当前登录用户 |
| `hotel_cart` | sessionStorage | 预订中转 cart |

## 📁 目录结构

```
hotel/
├── index.html           # 1 首页
├── list.html            # 2 列表
├── detail.html          # 3 详情
├── booking.html         # 4 预订
├── payment.html         # 5 支付
├── success.html         # 6 成功
├── orders.html          # 7 订单中心
├── order-detail.html    # 8 订单详情
├── me.html              # 9 我的
├── login.html           # 10 登录
├── README.md            # 本文档
└── assets/
    ├── styles.css       # 通用样式（设计 tokens + 组件）
    ├── data.js          # 模拟数据（6 酒店 + 6 城市 + 工具函数）
    └── components.js    # 通用组件（Nav/Footer/Frap/Toast）
```
