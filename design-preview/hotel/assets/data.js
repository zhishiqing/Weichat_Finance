// ============================================================
// Hotel Booking · Mock Data
// 用 localStorage 持久化订单数据
// ============================================================

const HOTELS = [
  {
    id: 'h001',
    nameCn: '上海外滩茂悦大酒店',
    nameEn: 'Hyatt Regency Shanghai Bund',
    city: '上海',
    district: '黄浦区·外滩',
    address: '上海市黄浦区黄浦路199号',
    star: 5,
    score: 9.2,
    reviewCount: 4823,
    price: 1288,
    oldPrice: 1588,
    tags: ['外滩江景', '地铁直达', '行政酒廊', '健身房', '游泳池'],
    cover: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
    images: [
      'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
      'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&q=80',
      'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&q=80',
      'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&q=80',
    ],
    rooms: [
      { id: 'r001-1', type: '豪华大床房', bed: '1.8m 大床', area: '38㎡', window: '有窗', price: 1288, breakfast: true, cancel: '免费取消' },
      { id: 'r001-2', type: '江景豪华房', bed: '1.8m 大床', area: '42㎡', window: '江景', price: 1688, breakfast: true, cancel: '免费取消' },
      { id: 'r001-3', type: '行政套房', bed: '1.8m + 1.5m', area: '68㎡', window: '江景', price: 2888, breakfast: true, cancel: '不可退' },
    ],
    reviews: [
      { user: '李**先生', score: 5, date: '2026-08-15', text: '外滩景观无敌，前台服务超棒，房间宽敞明亮，早餐丰富。下次还会选择这里。' },
      { user: '王**女士', score: 4, date: '2026-08-10', text: '位置方便，地铁 10 号线直达。房间干净整洁，性价比高。' },
      { user: '张**先生', score: 5, date: '2026-08-05', text: '夜景一流，黄浦江两岸尽收眼底。强烈推荐江景房。' },
    ],
    facilities: ['免费 WiFi', '停车场', '游泳池', '健身房', 'SPA', '餐厅', '酒吧', '会议室', '24小时前台', '行李寄存'],
  },
  {
    id: 'h002',
    nameCn: '北京三里屯通盈中心洲际酒店',
    nameEn: 'InterContinental Beijing Sanlitun',
    city: '北京',
    district: '朝阳区·三里屯',
    address: '北京市朝阳区南三里屯路1号',
    star: 5,
    score: 9.4,
    reviewCount: 3567,
    price: 1588,
    oldPrice: 1888,
    tags: ['三里屯', '时尚地标', 'SPA', '米其林餐厅'],
    cover: 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&q=80',
    images: [
      'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?w=800&q=80',
      'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&q=80',
      'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80',
    ],
    rooms: [
      { id: 'r002-1', type: '豪华房', bed: '1.8m 大床', area: '40㎡', window: '城景', price: 1588, breakfast: true, cancel: '免费取消' },
      { id: 'r002-2', type: '行政套房', bed: '1.8m 大床', area: '70㎡', window: '城景', price: 2688, breakfast: true, cancel: '免费取消' },
    ],
    reviews: [
      { user: '陈**先生', score: 5, date: '2026-08-12', text: '三里屯核心位置，逛街吃饭都方便。房间设计感强，服务一流。' },
    ],
    facilities: ['免费 WiFi', 'SPA', '健身房', '米其林餐厅', '酒吧', '24小时前台'],
  },
  {
    id: 'h003',
    nameCn: '杭州西子湖四季酒店',
    nameEn: 'Four Seasons Hangzhou at West Lake',
    city: '杭州',
    district: '西湖区·西湖',
    address: '杭州市西湖区灵隐路5号',
    star: 5,
    score: 9.6,
    reviewCount: 2890,
    price: 2888,
    oldPrice: 3288,
    tags: ['西湖景区', '园林酒店', '米其林', '下午茶'],
    cover: 'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&q=80',
    images: [
      'https://images.unsplash.com/photo-1611892440504-42a792e24d32?w=800&q=80',
      'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&q=80',
    ],
    rooms: [
      { id: 'r003-1', type: '园景房', bed: '1.8m 大床', area: '50㎡', window: '园景', price: 2888, breakfast: true, cancel: '免费取消' },
      { id: 'r003-2', type: '湖景套房', bed: '1.8m 大床', area: '85㎡', window: '湖景', price: 4688, breakfast: true, cancel: '免费取消' },
    ],
    reviews: [
      { user: '徐**女士', score: 5, date: '2026-08-20', text: '推开窗就是西湖，服务无微不至，园林景观美不胜收。' },
    ],
    facilities: ['免费 WiFi', '游泳池', 'SPA', '米其林餐厅', '茶室', '24小时管家'],
  },
  {
    id: 'h004',
    nameCn: '深圳湾木棉花酒店',
    nameEn: 'The Mumian Shenzhen',
    city: '深圳',
    district: '南山区·深圳湾',
    address: '深圳市南山区后海大道88号',
    star: 4,
    score: 8.8,
    reviewCount: 1245,
    price: 788,
    oldPrice: 988,
    tags: ['深圳湾', '春茧', '性价比'],
    cover: 'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&q=80',
    images: [
      'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=800&q=80',
    ],
    rooms: [
      { id: 'r004-1', type: '高级大床房', bed: '1.8m 大床', area: '32㎡', window: '城景', price: 788, breakfast: false, cancel: '免费取消' },
      { id: 'r004-2', type: '豪华套房', bed: '1.8m 大床', area: '55㎡', window: '海景', price: 1288, breakfast: true, cancel: '免费取消' },
    ],
    reviews: [
      { user: '吴**先生', score: 4, date: '2026-08-08', text: '位置好，靠近深圳湾公园。房间装修现代，性价比高。' },
    ],
    facilities: ['免费 WiFi', '健身房', '餐厅', '会议室'],
  },
  {
    id: 'h005',
    nameCn: '成都太古里博舍',
    nameEn: 'The Temple House Chengdu',
    city: '成都',
    district: '锦江区·太古里',
    address: '成都市锦江区中纱帽街8号',
    star: 5,
    score: 9.3,
    reviewCount: 1987,
    price: 1688,
    oldPrice: 1988,
    tags: ['太古里', '设计酒店', '历史建筑'],
    cover: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80',
    images: [
      'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=80',
    ],
    rooms: [
      { id: 'r005-1', type: '庭院房', bed: '1.8m 大床', area: '45㎡', window: '庭院', price: 1688, breakfast: true, cancel: '免费取消' },
    ],
    reviews: [
      { user: '刘**女士', score: 5, date: '2026-08-18', text: '设计感爆棚，由清代四合院改建而成，出行便利，体验独特。' },
    ],
    facilities: ['免费 WiFi', 'SPA', '健身房', '图书馆', '酒吧'],
  },
  {
    id: 'h006',
    nameCn: '丽江大研安缦',
    nameEn: 'Aman Lijiang',
    city: '丽江',
    district: '古城区·大研',
    address: '丽江市古城区光义街',
    star: 5,
    score: 9.7,
    reviewCount: 856,
    price: 4888,
    oldPrice: 5288,
    tags: ['古城', '雪山景', '私密度假', '安缦'],
    cover: 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&q=80',
    images: [
      'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800&q=80',
    ],
    rooms: [
      { id: 'r006-1', type: '庭院套房', bed: '1.8m 大床', area: '72㎡', window: '园景', price: 4888, breakfast: true, cancel: '不可退' },
    ],
    reviews: [
      { user: '黄**先生', score: 5, date: '2026-08-25', text: '庭院独栋，推窗就是玉龙雪山。安缦品质，无需多言。' },
    ],
    facilities: ['免费 WiFi', 'SPA', '游泳池', '茶室', '管家服务'],
  },
];

const CITIES = [
  { name: '上海', en: 'Shanghai', count: 1245, cover: 'https://images.unsplash.com/photo-1474181487882-5abf3f0ba6c2?w=400&q=80' },
  { name: '北京', en: 'Beijing', count: 985, cover: 'https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=400&q=80' },
  { name: '杭州', en: 'Hangzhou', count: 678, cover: 'https://images.unsplash.com/photo-1597918216043-1214f3d4d0d4?w=400&q=80' },
  { name: '深圳', en: 'Shenzhen', count: 562, cover: 'https://images.unsplash.com/photo-1503150082-3b4fa6c2c4d3?w=400&q=80' },
  { name: '成都', en: 'Chengdu', count: 432, cover: 'https://images.unsplash.com/photo-1591574843012-2e8f0c2b8c5a?w=400&q=80' },
  { name: '丽江', en: 'Lijiang', count: 289, cover: 'https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=400&q=80' },
];

const FILTERS = {
  star: [5, 4, 3],
  priceRange: [
    { label: '¥0-500', min: 0, max: 500 },
    { label: '¥500-1000', min: 500, max: 1000 },
    { label: '¥1000-2000', min: 1000, max: 2000 },
    { label: '¥2000+', min: 2000, max: Infinity },
  ],
  tags: ['免费取消', '含早餐', '江景/湖景', '近地铁', '亲子酒店', '商务中心'],
  sortOptions: [
    { key: 'recommend', label: '智能推荐' },
    { key: 'priceAsc', label: '价格 低到高' },
    { key: 'priceDesc', label: '价格 高到低' },
    { key: 'scoreDesc', label: '评分 高到低' },
  ],
};

// ============================================================
// 订单持久化
// ============================================================
const OrderStore = {
  KEY: 'hotel_orders',
  list() { return JSON.parse(localStorage.getItem(this.KEY) || '[]'); },
  save(orders) { localStorage.setItem(this.KEY, JSON.stringify(orders)); },
  add(order) {
    const orders = this.list();
    order.id = 'O' + Date.now();
    order.createTime = new Date().toISOString();
    orders.unshift(order);
    this.save(orders);
    return order;
  },
  get(id) { return this.list().find(o => o.id === id); },
  update(id, patch) {
    const orders = this.list();
    const idx = orders.findIndex(o => o.id === id);
    if (idx >= 0) {
      orders[idx] = { ...orders[idx], ...patch };
      this.save(orders);
      return orders[idx];
    }
    return null;
  },
};

// ============================================================
// URL 参数工具
// ============================================================
const URL = {
  get(key) { return new URLSearchParams(location.search).get(key); },
  set(key, value) {
    const u = new URLSearchParams(location.search);
    u.set(key, value);
    history.replaceState(null, '', '?' + u.toString());
  },
  toQS(obj) { return new URLSearchParams(obj).toString(); },
};

// ============================================================
// 工具函数
// ============================================================
function fmtDate(d) {
  if (!d) return '';
  const dt = new Date(d);
  return `${dt.getMonth() + 1}月${dt.getDate()}日`;
}
function fmtDateTime(d) {
  if (!d) return '';
  const dt = new Date(d);
  return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')} ${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`;
}
function nightsBetween(inD, outD) {
  const a = new Date(inD), b = new Date(outD);
  return Math.round((b - a) / 86400000);
}
function getHotel(id) { return HOTELS.find(h => h.id === id); }
function getRoom(hotelId, roomId) {
  const h = getHotel(hotelId);
  return h ? h.rooms.find(r => r.id === roomId) : null;
}
function getCart() {
  return JSON.parse(sessionStorage.getItem('hotel_cart') || '{}');
}
function setCart(cart) { sessionStorage.setItem('hotel_cart', JSON.stringify(cart)); }

function toast(msg) {
  const t = document.getElementById('toast');
  if (!t) { alert(msg); return; }
  t.textContent = msg;
  t.classList.add('visible');
  clearTimeout(t._tid);
  t._tid = setTimeout(() => t.classList.remove('visible'), 2500);
}

function getUser() { return JSON.parse(localStorage.getItem('hotel_user') || 'null'); }
function setUser(u) { localStorage.setItem('hotel_user', JSON.stringify(u)); }
function logout() { localStorage.removeItem('hotel_user'); }
