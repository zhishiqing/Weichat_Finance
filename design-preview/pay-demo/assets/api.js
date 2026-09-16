// ============================================================
// 前端 API 客户端 · Weichat_Finance
// ============================================================
//
// BASE_URL: 后端服务地址（context-path=/api）
//   - 本地开发：http://localhost:8080/api
//   - 通过 http-proxy 或 nginx 转发
//
// 自动能力：
//   - JSON 序列化
//   - R<T> 解包（throw on error）
//   - traceId 透传 + 控制台打印
//   - 超时 15s
//   - 401/403 跳登录（保留兼容位）
// ============================================================

// BASE_URL: 后端服务地址（context-path=/api）
//   - Mock 模式（默认，无后端环境）：http://localhost:18080/api
//   - 真实 Java 后端：http://localhost:8080/api
// 切换方式：URL 加 ?real=1 走真实后端
const useReal = new URLSearchParams(location.search).get('real') === '1';
const BASE_URL = useReal
  ? 'http://localhost:8080/api'
  : 'http://localhost:18080/api';
const TIMEOUT_MS = 15000;

// 每个请求生成 traceId（12 位随机），与后端 MDC 串联
function newTraceId() {
  return 'fe-' + Math.random().toString(36).slice(2, 14);
}

function showError(msg) {
  if (typeof toast === 'function') toast(msg);
  else console.error('[API]', msg);
}

// 核心 fetch 封装
async function request(method, path, { body, query, headers = {} } = {}) {
  const traceId = newTraceId();
  const url = new URL(BASE_URL + path);
  if (query) {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null) url.searchParams.set(k, v);
    });
  }

  const ctrl = new AbortController();
  const tid = setTimeout(() => ctrl.abort(), TIMEOUT_MS);

  const finalHeaders = {
    'Content-Type': 'application/json',
    'X-Trace-Id': traceId,
    ...headers,
  };

  const init = {
    method,
    headers: finalHeaders,
    signal: ctrl.signal,
  };
  if (body !== undefined && method !== 'GET') init.body = JSON.stringify(body);

  let res, data;
  try {
    console.log(`[API ${traceId}] ${method} ${url}`);
    res = await fetch(url, init);
  } catch (e) {
    clearTimeout(tid);
    if (e.name === 'AbortError') {
      showError(`请求超时 (${TIMEOUT_MS / 1000}s) · ${path}`);
      throw new Error('TIMEOUT');
    }
    showError(`网络异常：${e.message} · ${path}`);
    throw e;
  }
  clearTimeout(tid);

  // 后端 R<T> 响应解析
  try {
    data = await res.json();
  } catch (e) {
    showError(`响应解析失败：${res.status} · ${path}`);
    throw new Error('PARSE_FAIL');
  }

  console.log(`[API ${traceId}] <-- ${data.code} ${data.message || ''}`, data);

  if (data.code !== 200) {
    const codeMap = {
      400: '参数错误',
      401: '请先登录',
      403: '权限不足',
      404: '资源不存在',
      409: '重复请求',
      500: '服务异常',
      502: '微信服务端错误',
    };
    const msg = data.message || codeMap[data.code] || '请求失败';
    showError(`[${data.code}] ${msg}`);
    const err = new Error(msg);
    err.code = data.code;
    err.traceId = traceId;
    throw err;
  }

  return { data: data.data, traceId };
}

// ============================================================
// API 业务方法
// ============================================================
const API = {
  // 健康检查
  health: () => request('GET', '/health'),

  // 商户
  getMerchant: (mchId) => request('GET', `/v1/merchant/${mchId}`),
  listMerchants: () => request('GET', '/v1/merchant'),
  listPartners: () => request('GET', '/v1/merchant/partner/list'),

  // JSAPI 支付
  createJsapiOrder: (req) => request('POST', '/v1/payment/jsapi/create', { body: req }),
  queryOrder: (outTradeNo) => request('GET', `/v1/payment/order/${outTradeNo}`),
  closeOrder: (outTradeNo) => request('POST', `/v1/payment/order/${outTradeNo}/close`),

  // Native 支付
  createNativeOrder: (req) => request('POST', '/v1/payment/native/create', { body: req }),

  // 退款
  createRefund: (req) => request('POST', '/v1/payment/refund/create', { body: req }),
  queryRefund: (outRefundNo) => request('GET', `/v1/payment/refund/${outRefundNo}`),

  // ---- 对账（演示用） ----
  listReconciliations: () => request('GET', '/v1/reconciliation/list'),
  triggerReconciliation: () => request('POST', '/v1/reconciliation/trigger'),
};

// ============================================================
// 本地存储辅助
// ============================================================
const Store = {
  setOrder(order) {
    const list = this.listOrders();
    order.id = order.id || ('O' + Date.now());
    order.createTime = order.createTime || new Date().toISOString();
    list.unshift(order);
    sessionStorage.setItem('wxpay_orders', JSON.stringify(list.slice(0, 50)));
    return order;
  },
  listOrders() { return JSON.parse(sessionStorage.getItem('wxpay_orders') || '[]'); },
  getOrder(id) { return this.listOrders().find(o => o.id === id); },
  updateOrder(id, patch) {
    const list = this.listOrders();
    const idx = list.findIndex(o => o.id === id);
    if (idx >= 0) {
      list[idx] = { ...list[idx], ...patch };
      sessionStorage.setItem('wxpay_orders', JSON.stringify(list));
      return list[idx];
    }
    return null;
  },
};

window.API = API;
window.Store = Store;
