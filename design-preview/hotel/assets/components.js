// ============================================================
// 通用组件渲染函数（Nav / Footer / Frap）
// ============================================================

function renderNav(active = '') {
  const user = getUser();
  return `
    <nav class="global-nav">
      <a href="index.html" class="brand">
        <div class="brand-logo">H</div>
        <div>
          <div class="brand-text">Hotel Booking</div>
          <div class="brand-text-sub">星巴克风格酒店预订</div>
        </div>
      </a>
      <div class="nav-links">
        <a href="index.html" class="nav-link ${active==='home'?'active':''}">首页</a>
        <a href="list.html" class="nav-link ${active==='list'?'active':''}">酒店</a>
        <a href="orders.html" class="nav-link ${active==='orders'?'active':''}">订单</a>
        <a href="me.html" class="nav-link ${active==='me'?'active':''}">我的</a>
        ${user
          ? `<a href="me.html" class="nav-cta">${user.name}</a>`
          : `<a href="login.html" class="nav-cta">登录</a>`
        }
      </div>
    </nav>
  `;
}

function renderFooter() {
  return `
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-brand">
          <div class="brand-logo" style="width:3rem;height:3rem;font-size:1.4rem;">H</div>
          Hotel Booking
        </div>
        <div class="footer-links">
          <a href="#">帮助中心</a>
          <a href="#">服务协议</a>
          <a href="#">隐私政策</a>
          <a href="#">联系客服 400-100-1234</a>
        </div>
        <div>© 2026 Hotel Booking · Demo · 风格参考 Starbucks</div>
      </div>
    </footer>
  `;
}

function renderFrap() {
  return `
    <button class="frap" onclick="toast('客服小助手：您好，请问需要什么帮助？')" title="联系客服">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
      </svg>
    </button>
  `;
}

function renderToast() {
  return `<div class="toast" id="toast"></div>`;
}

function mountCommon(active) {
  document.getElementById('nav-slot').outerHTML = renderNav(active);
  document.getElementById('footer-slot').outerHTML = renderFooter();
  const frapSlot = document.getElementById('frap-slot');
  if (frapSlot) frapSlot.outerHTML = renderFrap();
  const toastSlot = document.getElementById('toast-slot');
  if (toastSlot) toastSlot.outerHTML = renderToast();
}

function requireLogin() {
  const u = getUser();
  if (!u) {
    toast('请先登录');
    setTimeout(() => location.href = 'login.html?redirect=' + encodeURIComponent(location.href), 800);
    return false;
  }
  return true;
}
