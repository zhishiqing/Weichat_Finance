// ============================================================
// Mock 后端服务 · 让 pay-demo 前端无需 Java 后端即可联调
// 复刻 R<T> 响应格式 + 状态流转
// ============================================================
const http = require('http');
const { URL } = require('url');

const PORT = 18080;

const orders = new Map();
const refunds = new Map();
const merchants = [
  { id: 1, mchId: '1674723182', merchantName: '默认直连商户', appId: 'wx0000000000000000', apiV3Key: '********************************', certSerialNo: '1234567890ABCDEF', notifyUrlBase: 'http://localhost:18080', enabled: 1, gmtCreate: '2026-09-14T10:00:00', gmtModified: '2026-09-14T10:00:00', isDeleted: 0 },
  { id: 2, mchId: '1900000109', merchantName: '测试服务商', appId: 'wx9999999999999999', apiV3Key: '********************************', certSerialNo: 'FEDCBA0987654321', notifyUrlBase: 'http://localhost:18080', enabled: 1, gmtCreate: '2026-09-14T10:00:00', gmtModified: '2026-09-14T10:00:00', isDeleted: 0, mode: 'PARTNER' },
];

function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = Math.random() * 16 | 0;
    return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
  });
}

function R(code, message, data) {
  return { code, message: message || (code === 200 ? 'success' : 'fail'), data: data || null };
}

function withCORS(req, res) {
  res.setHeader('Access-Control-Allow-Origin', req.headers.origin || '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, X-Trace-Id, Authorization');
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Expose-Headers', 'X-Trace-Id');
}

function send(res, code, body, traceId) {
  res.writeHead(code, {
    'Content-Type': 'application/json; charset=utf-8',
    'X-Trace-Id': traceId || '',
  });
  res.end(JSON.stringify(body));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let chunks = [];
    req.on('data', c => chunks.push(c));
    req.on('end', () => {
      const text = Buffer.concat(chunks).toString();
      try { resolve(text ? JSON.parse(text) : {}); } catch (e) { reject(e); }
    });
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  withCORS(req, res);
  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }

  const traceId = req.headers['x-trace-id'] || ('be-' + Math.random().toString(36).slice(2, 14));
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const path = url.pathname;
  const method = req.method;

  console.log(`[${new Date().toISOString()}] [${traceId}] ${method} ${path}`);

  try {
    // ---------- 健康检查 ----------
    if (path === '/api/health' && method === 'GET') {
      return send(res, 200, R(200, 'success', { status: 'UP', db: 'UP' }), traceId);
    }

    // ---------- 商户 ----------
    if (path === '/api/v1/merchant' && method === 'GET') {
      const enabled = merchants.filter(m => m.enabled === 1);
      return send(res, 200, R(200, 'success', enabled), traceId);
    }
    if (path.startsWith('/api/v1/merchant/') && method === 'GET') {
      const mchId = decodeURIComponent(path.split('/').pop());
      const m = merchants.find(x => x.mchId === mchId);
      if (!m) return send(res, 200, R(404, '商户不存在：' + mchId, null), traceId);
      return send(res, 200, R(200, 'success', m), traceId);
    }
    if (path === '/api/v1/merchant/partner/list' && method === 'GET') {
      return send(res, 200, R(200, 'success', merchants.filter(m => m.mode === 'PARTNER')), traceId);
    }

    // ---------- 创建 JSAPI 订单 ----------
    if (path === '/api/v1/payment/jsapi/create' && method === 'POST') {
      const body = await readBody(req);
      if (!body.outTradeNo || !body.description || !body.amountTotal) return send(res, 200, R(400, '参数缺失', null), traceId);
      if (body.amountTotal <= 0) return send(res, 200, R(400, '金额必须 > 0', null), traceId);
      if (!body.openid) return send(res, 200, R(400, 'JSAPI 必须传 openid', null), traceId);

      if (orders.has(body.outTradeNo)) return send(res, 200, R(409, '商户订单号已存在', null), traceId);

      const order = {
        outTradeNo: body.outTradeNo,
        mchId: '1674723182',
        appId: 'wx0000000000000000',
        description: body.description,
        amountTotal: body.amountTotal,
        currency: body.currency || 'CNY',
        openid: body.openid,
        productType: 'JSAPI',
        attach: body.attach || null,
        payStatus: 'CREATED',
        source: 'MOCK',
        prepayId: 'MOCK_prepay_' + uuid().slice(0, 16),
        transactionId: null,
        gmtCreate: new Date().toISOString(),
      };
      orders.set(body.outTradeNo, order);

      // 模拟：30 秒后自动变 PAID
      setTimeout(() => {
        if (orders.has(body.outTradeNo)) {
          const o = orders.get(body.outTradeNo);
          if (o.payStatus === 'CREATED' || o.payStatus === 'NOTPAY') {
            o.payStatus = 'PAID';
            o.transactionId = '420000' + Date.now();
            o.paidAt = new Date().toISOString();
            console.log(`  └─ 自动模拟支付成功: ${body.outTradeNo}`);
          }
        }
      }, 30000);

      return send(res, 200, R(200, 'success', { prepayId: order.prepayId, source: 'MOCK' }), traceId);
    }

    // ---------- 创建 Native 订单 ----------
    if (path === '/api/v1/payment/native/create' && method === 'POST') {
      const body = await readBody(req);
      if (!body.outTradeNo || !body.amountTotal) return send(res, 200, R(400, '参数缺失', null), traceId);
      if (orders.has(body.outTradeNo)) return send(res, 200, R(409, '商户订单号已存在', null), traceId);

      const order = {
        outTradeNo: body.outTradeNo,
        mchId: '1674723182',
        appId: 'wx0000000000000000',
        description: body.description,
        amountTotal: body.amountTotal,
        currency: body.currency || 'CNY',
        productType: 'NATIVE',
        payStatus: 'CREATED',
        source: 'MOCK',
        codeUrl: 'weixin://wxpay/bizpayurl?pr=MOCK_' + uuid().slice(0, 10),
        gmtCreate: new Date().toISOString(),
      };
      orders.set(body.outTradeNo, order);

      // 模拟 30 秒后自动 PAID
      setTimeout(() => {
        if (orders.has(body.outTradeNo)) {
          const o = orders.get(body.outTradeNo);
          if (o.payStatus === 'CREATED' || o.payStatus === 'NOTPAY') {
            o.payStatus = 'PAID';
            o.transactionId = '420000' + Date.now();
            o.paidAt = new Date().toISOString();
          }
        }
      }, 30000);

      return send(res, 200, R(200, 'success', { codeUrl: order.codeUrl, source: 'MOCK' }), traceId);
    }

    // ---------- 查单 ----------
    const queryMatch = path.match(/^\/api\/v1\/payment\/order\/(.+)$/);
    if (queryMatch && method === 'GET') {
      const otn = decodeURIComponent(queryMatch[1]);
      const order = orders.get(otn);
      if (!order) return send(res, 200, R(404, '订单不存在', null), traceId);
      return send(res, 200, R(200, 'success', order), traceId);
    }

    // ---------- 关单 ----------
    const closeMatch = path.match(/^\/api\/v1\/payment\/order\/(.+)\/close$/);
    if (closeMatch && method === 'POST') {
      const otn = decodeURIComponent(closeMatch[1]);
      const order = orders.get(otn);
      if (!order) return send(res, 200, R(404, '订单不存在', null), traceId);
      order.payStatus = 'CLOSED';
      order.closedAt = new Date().toISOString();
      return send(res, 200, R(200, 'success', null), traceId);
    }

    // ---------- 退款 ----------
    if (path === '/api/v1/payment/refund/create' && method === 'POST') {
      const body = await readBody(req);
      if (!body.outRefundNo || !body.outTradeNo) return send(res, 200, R(400, '参数缺失', null), traceId);
      if (refunds.has(body.outRefundNo)) return send(res, 200, R(409, '商户退款单号已存在', null), traceId);
      const order = orders.get(body.outTradeNo);
      if (!order) return send(res, 200, R(404, '原订单不存在', null), traceId);
      if (body.amountRefund > order.amountTotal) return send(res, 200, R(400, '退款金额超限', null), traceId);

      const refund = {
        outRefundNo: body.outRefundNo,
        outTradeNo: body.outTradeNo,
        amountRefund: body.amountRefund,
        amountTotal: body.amountTotal,
        reason: body.reason,
        refundStatus: 'SUCCESS',
        refundId: 'RFD' + Date.now(),
        gmtCreate: new Date().toISOString(),
      };
      refunds.set(body.outRefundNo, refund);
      order.payStatus = 'REFUNDED';

      return send(res, 200, R(200, 'success', refund), traceId);
    }

    // ---------- 查退款 ----------
    const rqMatch = path.match(/^\/api\/v1\/payment\/refund\/(.+)$/);
    if (rqMatch && method === 'GET') {
      const orn = decodeURIComponent(rqMatch[1]);
      const refund = refunds.get(orn);
      if (!refund) return send(res, 200, R(404, '退款单不存在', null), traceId);
      return send(res, 200, R(200, 'success', refund), traceId);
    }

    // 404
    send(res, 404, R(404, '接口不存在', null), traceId);

  } catch (e) {
    console.error('  └─ error:', e);
    send(res, 500, R(500, e.message, null), traceId);
  }
});

server.listen(PORT, () => {
  console.log(`🟢 Mock 后端启动成功`);
  console.log(`   URL:     http://localhost:${PORT}`);
  console.log(`   Context: /api`);
  console.log(`   健康检查: http://localhost:${PORT}/api/health`);
});
