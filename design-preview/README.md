# Design Preview · Weichat Finance

参考 [Starbucks DESIGN.md](../DESIGN.md) 设计系统的 UI 预览页。

## 文件

| 文件 | 说明 |
|---|---|
| `login.html` | 商户登录页（暖奶油背景 + House Green 导航 + 浮动 label + 50px 全 pill 按钮） |
| `payment-success.html` | 支付成功页（深绿 hero + 弹入对勾 + 白色订单卡 + Frap 浮动按钮） |
| `screenshots/01-login-desktop.png` | 登录页桌面端截图 |
| `screenshots/02-payment-success-desktop.png` | 支付成功页桌面端截图 |

## 设计 tokens（核心）

```css
/* 四层绿色系统 */
--starbucks-green: #006241;    /* h1 标题 */
--green-accent:    #00754A;    /* 主 CTA + Frap */
--house-green:     #1E3932;    /* 深绿导航/Hero/Footer */
--green-uplift:    #2b5148;    /* 次级装饰 */

/* 暖色背景 */
--neutral-warm: #f2f0eb;       /* 主页面背景（不是纯白！） */
--ceramic:      #edebe9;       /* 区域分隔 */
--white:        #ffffff;       /* 卡片表面 */

/* 圆角 + 阴影 */
--card-radius:  12px;          /* 卡片/Modal */
--button-radius: 50px;         /* 所有按钮（全 pill） */
--card-shadow: 0 0 0.5px rgba(0,0,0,0.14), 0 1px 1px rgba(0,0,0,0.24);
--frap-shadow: 0 0 6px rgba(0,0,0,0.24), 0 8px 12px rgba(0,0,0,0.14);

/* 字体 */
font-family: 'Inter', "Helvetica Neue", Helvetica, Arial, sans-serif;
letter-spacing: -0.01em;       /* 紧凑跟踪 */
```

## 本地预览

```bash
# 方式 1：直接打开
start design-preview/login.html    # Windows

# 方式 2：起本地服务
npx http-server design-preview -p 8090 -c-1
# → http://localhost:8090/login.html
# → http://localhost:8090/payment-success.html
```

## 设计哲学（来自 Starbucks DESIGN.md）

- ✅ **暖奶油背景** `#f2f0eb`（不是纯白）—— 让界面有"咖啡馆"的温度
- ✅ **四层绿色系统** —— 不用一个绿，而是按角色映射
- ✅ **50px 全 pill 按钮** —— 所有按钮统一形状
- ✅ **scale(0.95) 按下反馈** —— 标志性微交互
- ✅ **Frap 浮动圆形 CTA** —— 56px 圆形 + 双层阴影
- ✅ **layered low-alpha shadows** —— 用 2-3 层低透明度阴影营造层次（不是单层重阴影）
- ❌ **不要**：纯白背景、单色绿、方角按钮、单层重阴影
