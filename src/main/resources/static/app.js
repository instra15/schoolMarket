/* ==========================================================
   校园二手交易平台 —— 前端逻辑
   纯原生 JS，无框架。后端返回统一结构 {code, message, data}。
   ========================================================== */

// ---------- 全局状态 ----------
const state = {
    user: null,          // 当前登录用户
    pageNum: 1,
    pageSize: 12,
    totalPages: 0,
    categories: [],
    currentTab: 'market'
};

// ==========================================================
//  基础工具
// ==========================================================

/**
 * 统一的 API 调用。
 *
 * 关键点：credentials:'same-origin' —— 后端用 Session 存登录态，
 * 跨请求必须携带 JSESSIONID Cookie，否则 @CurrentUser 拿不到 userId。
 * 同源请求浏览器默认会带，但显式写上更保险（尤其在 file:// 打开时）。
 */
async function api(url, options = {}) {
    const config = {
        method: options.method || 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        ...options
    };

    if (config.body && typeof config.body !== 'string') {
        config.body = JSON.stringify(config.body);
    }

    let resp;
    try {
        resp = await fetch(url, config);
    } catch (e) {
        throw { code: -1, message: '网络异常，请检查服务是否启动' };
    }

    let json;
    try {
        json = await resp.json();
    } catch (e) {
        throw { code: resp.status, message: '响应格式错误' };
    }
    return json;
}

/** 弹出提示 */
let toastTimer = null;
function toast(message, type = '') {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.className = 'toast show ' + type;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.className = 'toast ' + type; }, 2600);
}

/** 金额格式化 */
function money(value) {
    if (value === null || value === undefined) return '0.00';
    return Number(value).toFixed(2);
}

/** 时间格式化：后端返回 "2026-10-28 17:30:00" */
function formatTime(str) {
    if (!str) return '';
    return String(str).replace('T', ' ').substring(0, 16);
}

/** HTML 转义，防止用户输入的内容破坏页面结构 */
function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

// ==========================================================
//  弹窗
// ==========================================================

function openModal(name) {
    document.getElementById(name + 'Modal').classList.add('show');
    // 清空错误提示
    const err = document.getElementById(name + 'Error');
    if (err) err.textContent = '';
}

function closeModal(name) {
    document.getElementById(name + 'Modal').classList.remove('show');
}

/** 点击遮罩空白处关闭 */
document.querySelectorAll('.modal-mask').forEach(mask => {
    mask.addEventListener('click', e => {
        if (e.target === mask) mask.classList.remove('show');
    });
});

// ==========================================================
//  登录态
// ==========================================================

/**
 * 用 GET /api/users/me 作为登录态探针。
 *
 * 为什么不用前端存 userId：后端用 Session 存登录态，
 * 前端自己记一个 userId 会出现「前端显示已登录、后端认为未登录」的错觉。
 * 直接问后端要最准。未登录时后端返回 401。
 */
async function refreshLoginState() {
    const res = await api('/api/users/me');
    if (res.code === 200 && res.data) {
        setUser(res.data);
    } else {
        setUser(null);
    }
    return state.user;
}

function setUser(user) {
    state.user = user;
    const guestBar = document.getElementById('guestBar');
    const userBar = document.getElementById('userBar');

    if (user) {
        guestBar.hidden = true;
        userBar.hidden = false;
        document.getElementById('nickname').textContent = user.nickname || user.username;
        document.getElementById('avatarChar').textContent =
            (user.nickname || user.username || '?').charAt(0);
    } else {
        guestBar.hidden = false;
        userBar.hidden = true;
    }
}

async function doLogin(event) {
    event.preventDefault();
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    const errEl = document.getElementById('loginError');
    errEl.textContent = '';

    const res = await api('/api/users/login', {
        method: 'POST',
        body: { username, password }
    });

    if (res.code !== 200) {
        errEl.textContent = res.message || '登录失败';
        return false;
    }

    closeModal('loginModal');
    document.getElementById('loginPassword').value = '';
    await refreshLoginState();
    toast('登录成功', 'success');
    loadUnreadCount();
    return false;
}

async function doRegister(event) {
    event.preventDefault();
    const body = {
        username: document.getElementById('regUsername').value.trim(),
        nickname: document.getElementById('regNickname').value.trim(),
        phone: document.getElementById('regPhone').value.trim(),
        password: document.getElementById('regPassword').value
    };
    const errEl = document.getElementById('registerError');
    errEl.textContent = '';

    const res = await api('/api/users/register', { method: 'POST', body });

    if (res.code !== 200) {
        errEl.textContent = res.message || '注册失败';
        return false;
    }

    closeModal('registerModal');
    toast('注册成功，请登录', 'success');
    document.getElementById('loginUsername').value = body.username;
    openModal('login');
    return false;
}

async function logout() {
    await api('/api/users/logout', { method: 'POST' });
    setUser(null);
    document.getElementById('unreadBadge').hidden = true;
    toast('已退出登录');
}

// ==========================================================
//  页面切换
// ==========================================================

async function switchTab(tab) {
    state.currentTab = tab;

    document.querySelectorAll('.nav-item').forEach(el => {
        el.classList.toggle('active', el.dataset.tab === tab);
    });
    document.querySelectorAll('.tab-panel').forEach(el => {
        el.classList.toggle('active', el.id === 'panel-' + tab);
    });

    if (tab === 'market') {
        loadProducts(1);
    } else if (tab === 'sell') {
        // 先校验登录，未登录就别往下走
        if (!state.user) {
            toast('请先登录');
            switchTab('market');
            openModal('login');
            return;
        }
        loadCategories();
    } else if (tab === 'orders') {
        // 重置状态筛选为「全部」，避免沿用上次的筛选条件
        document.querySelectorAll('#panel-orders .chip').forEach((chip, i) => {
            chip.classList.toggle('active', i === 0);
        });
        loadOrders('buy', '');
    } else if (tab === 'sold') {
        loadOrders('sell', '');
    } else if (tab === 'notifications') {
        loadNotifications();
    }
}

// ==========================================================
//  分类
// ==========================================================

async function loadCategories() {
    if (state.categories.length) return;
    const res = await api('/api/categories');
    if (res.code !== 200) return;
    state.categories = res.data || [];

    const options = state.categories
        .map(c => `<option value="${c.id}">${escapeHtml(c.name)}（${c.productCount ?? 0}）</option>`)
        .join('');

    document.getElementById('categoryFilter').innerHTML =
        '<option value="">全部分类</option>' + options;
    document.getElementById('pCategory').innerHTML =
        '<option value="">请选择分类</option>' + options;
}

// ==========================================================
//  商品列表
// ==========================================================

async function loadProducts(pageNum) {
    state.pageNum = pageNum || 1;

    const params = new URLSearchParams({
        pageNum: state.pageNum,
        pageSize: state.pageSize
    });

    const keyword = document.getElementById('keyword').value.trim();
    const categoryId = document.getElementById('categoryFilter').value;
    const minPrice = document.getElementById('minPrice').value.trim();
    const maxPrice = document.getElementById('maxPrice').value.trim();

    if (keyword) params.set('keyword', keyword);
    if (categoryId) params.set('categoryId', categoryId);
    if (minPrice) params.set('minPrice', minPrice);
    if (maxPrice) params.set('maxPrice', maxPrice);

    const grid = document.getElementById('productGrid');
    grid.innerHTML = '<div class="loading">加载中…</div>';

    const res = await api('/api/products?' + params.toString());

    if (res.code !== 200) {
        grid.innerHTML = '';
        toast(res.message || '加载失败', 'error');
        return;
    }

    const page = res.data || { list: [], total: 0 };
    const list = page.list || [];
    state.totalPages = Math.ceil((page.total || 0) / state.pageSize);

    document.getElementById('productEmpty').hidden = list.length > 0;

    if (!list.length) {
        grid.innerHTML = '';
    } else {
        grid.innerHTML = list.map(productCard).join('');
    }

    // 分页控件
    const pager = document.getElementById('productPager');
    pager.hidden = state.totalPages <= 1;
    document.getElementById('pageInfo').textContent =
        `第 ${state.pageNum} / ${state.totalPages} 页，共 ${page.total} 件`;
    document.getElementById('prevPage').disabled = state.pageNum <= 1;
    document.getElementById('nextPage').disabled = state.pageNum >= state.totalPages;
}

function productCard(p) {
    const img = p.imageUrl
        ? `<img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.title)}" onerror="this.parentNode.innerHTML='&#128230;'">`
        : '&#128230;';

    let tag = '<span class="status-tag">在售</span>';
    if (p.status === 'OFF_SALE') {
        tag = '<span class="status-tag off">已下架</span>';
    } else if (p.stock === 0) {
        tag = '<span class="status-tag sold-out">已售罄</span>';
    }

    const categoryName = p.category ? escapeHtml(p.category.name) : '未分类';
    const sellerName = p.seller ? escapeHtml(p.seller.nickname) : '未知卖家';

    return `
        <div class="product-card" onclick="showDetail(${p.id})">
            <div class="product-thumb">${img}${tag}</div>
            <div class="product-info">
                <div class="product-title">${escapeHtml(p.title)}</div>
                <div class="product-meta">
                    <span>${categoryName}</span>
                    <span>·</span>
                    <span>${sellerName}</span>
                </div>
                <div class="product-price"><small>¥</small>${money(p.price)}</div>
                <div class="product-stock">库存 ${p.stock} 件</div>
            </div>
        </div>`;
}

function changePage(delta) {
    const next = state.pageNum + delta;
    if (next < 1 || next > state.totalPages) return;
    loadProducts(next);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function resetFilter() {
    document.getElementById('keyword').value = '';
    document.getElementById('categoryFilter').value = '';
    document.getElementById('minPrice').value = '';
    document.getElementById('maxPrice').value = '';
    loadProducts(1);
}

// ==========================================================
//  商品详情 + 购买
// ==========================================================

async function showDetail(id) {
    const res = await api('/api/products/' + id);
    if (res.code !== 200) {
        toast(res.message || '商品不存在', 'error');
        return;
    }
    const p = res.data;

    const img = p.imageUrl
        ? `<img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.title)}" onerror="this.parentNode.innerHTML='&#128230;'">`
        : '&#128230;';

    const canBuy = p.status === 'ON_SALE' && p.stock > 0;
    const isMine = state.user && p.seller && p.seller.id === state.user.id;

    document.getElementById('detailBody').innerHTML = `
        <div class="detail-img">${img}</div>
        <div class="detail-title">${escapeHtml(p.title)}</div>
        <div class="detail-price"><small>¥</small>${money(p.price)}</div>
        ${p.description ? `<div class="detail-desc">${escapeHtml(p.description)}</div>` : ''}
        <div class="detail-rows">
            <div class="detail-row"><span class="k">分类</span><span class="v">${p.category ? escapeHtml(p.category.name) : '未分类'}</span></div>
            <div class="detail-row"><span class="k">卖家</span><span class="v">${p.seller ? escapeHtml(p.seller.nickname) : '未知'}</span></div>
            <div class="detail-row"><span class="k">库存</span><span class="v">${p.stock} 件</span></div>
            <div class="detail-row"><span class="k">状态</span><span class="v">${p.status === 'ON_SALE' ? '在售' : '已下架'}</span></div>
            <div class="detail-row"><span class="k">发布</span><span class="v">${formatTime(p.createTime)}</span></div>
        </div>
        <div class="buy-bar">
            <input type="number" id="buyQty" class="input" value="1" min="1" max="${p.stock}">
            <button class="btn btn-primary" ${canBuy ? '' : 'disabled'}
                    onclick="buyProduct(${p.id})">${canBuy ? '立即购买' : (p.stock === 0 ? '已售罄' : '已下架')}</button>
        </div>
        ${isMine ? '<p class="form-tip">这是你发布的商品</p>' : ''}
    `;

    openModal('detailModal');
}

async function buyProduct(productId) {
    if (!state.user) {
        toast('请先登录');
        closeModal('detailModal');
        openModal('login');
        return;
    }

    // buyQty 是详情弹窗动态生成的，取不到说明弹窗未渲染，用默认 1
    const qtyEl = document.getElementById('buyQty');
    const qty = qtyEl ? (parseInt(qtyEl.value, 10) || 1) : 1;

    const res = await api('/api/orders', {
        method: 'POST',
        body: { items: [{ productId, quantity: qty }] }
    });

    if (res.code !== 200) {
        // 409 = 库存不足/被抢先购买，这是并发下的正常结果，给出明确提示
        toast(res.message || '购买失败', res.code === 409 ? 'error' : 'error');
        return;
    }

    closeModal('detailModal');
    toast('下单成功，订单号 ' + res.data, 'success');
    loadProducts(state.pageNum);
    loadUnreadCount();
}

// ==========================================================
//  发布商品
// ==========================================================

async function publishProduct(event) {
    event.preventDefault();
    if (!state.user) { toast('请先登录'); openModal('login'); return false; }

    const body = {
        title: document.getElementById('pTitle').value.trim(),
        description: document.getElementById('pDesc').value.trim(),
        imageUrl: document.getElementById('pImage').value.trim(),
        price: parseFloat(document.getElementById('pPrice').value),
        stock: parseInt(document.getElementById('pStock').value, 10),
        categoryId: parseInt(document.getElementById('pCategory').value, 10)
    };

    if (!body.title || !body.price || !body.stock || !body.categoryId) {
        toast('请填写完整的商品信息');
        return false;
    }

    const res = await api('/api/products', { method: 'POST', body });

    if (res.code !== 200) {
        toast(res.message || '发布失败', 'error');
        return false;
    }

    toast('发布成功', 'success');
    document.getElementById('publishForm').reset();
    document.getElementById('pStock').value = '1';
    switchTab('market');
    return false;
}

// ==========================================================
//  订单
// ==========================================================

async function loadOrders(mode, status) {
    const isBuy = mode === 'buy';
    const listEl = document.getElementById(isBuy ? 'orderList' : 'soldList');
    const emptyEl = document.getElementById(isBuy ? 'orderEmpty' : 'soldEmpty');

    const params = new URLSearchParams({ pageNum: 1, pageSize: 20 });
    if (status) params.set('status', status);

    // 买家视角是 /api/orders，卖家视角是 /api/orders/sold
    const path = isBuy ? '/api/orders' : '/api/orders/sold';
    const res = await api(path + '?' + params.toString());
    if (res.code !== 200) {
        toast(res.message || '加载失败', 'error');
        return;
    }

    const list = (res.data && res.data.list) || [];
    emptyEl.hidden = list.length > 0;
    listEl.innerHTML = list.length ? list.map(o => orderCard(o, isBuy)).join('') : '';
}

function orderCard(o, isBuy) {
    const stateText = { PENDING: '待支付', PAID: '已支付', CANCELLED: '已取消' };
    const items = (o.items || []).map(i => `
        <div class="order-item">
            <span class="order-item-name">${escapeHtml(i.productTitle || ('商品 #' + i.productId))}</span>
            <span class="order-item-qty">×${i.quantity}</span>
            <span class="order-item-price">¥${money(i.price)}</span>
        </div>`).join('');

    // 买家视角才有支付/取消按钮
    let actions = '';
    if (isBuy && o.status === 'PENDING') {
        actions = `
            <button class="btn btn-ghost btn-sm" onclick="cancelOrder('${escapeHtml(o.orderNo)}')">取消</button>
            <button class="btn btn-primary btn-sm" onclick="payOrder('${escapeHtml(o.orderNo)}')">去支付</button>`;
    }

    return `
        <div class="order-card">
            <div class="order-head">
                <span>订单号 <span class="order-no">${escapeHtml(o.orderNo)}</span></span>
                <span>${formatTime(o.createTime)}</span>
                <span class="order-state state-${o.status}">${stateText[o.status] || o.status}</span>
            </div>
            <div class="order-items">${items || '<div class="order-item"><span class="order-item-name">（无订单项）</span></div>'}</div>
            <div class="order-foot">
                <span class="order-total">共 ${(o.items || []).reduce((s, i) => s + i.quantity, 0)} 件，合计 <b>¥${money(o.totalPrice)}</b></span>
                <div class="order-actions">${actions}</div>
            </div>
        </div>`;
}

async function payOrder(orderNo) {
    const res = await api('/api/orders/' + orderNo + '/pay', { method: 'PUT' });
    if (res.code !== 200) {
        toast(res.message || '支付失败', 'error');
        return;
    }
    toast('支付成功', 'success');
    loadOrders('buy', currentStatus());
    loadUnreadCount();
}

async function cancelOrder(orderNo) {
    if (!confirm('确定取消该订单吗？库存将被回补。')) return;
    const res = await api('/api/orders/' + orderNo + '/cancel', { method: 'PUT' });
    if (res.code !== 200) {
        toast(res.message || '取消失败', 'error');
        return;
    }
    toast('订单已取消', 'success');
    loadOrders('buy', currentStatus());
    loadUnreadCount();
}

/** 取当前选中的状态筛选 */
function currentStatus() {
    const active = document.querySelector('#panel-orders .chip.active');
    return active ? active.dataset.status : '';
}

// ==========================================================
//  通知
// ==========================================================

async function loadNotifications() {
    const res = await api('/api/notifications?pageNum=1&pageSize=30');
    if (res.code !== 200) {
        toast(res.message || '加载失败', 'error');
        return;
    }
    const list = (res.data && res.data.list) || [];
    document.getElementById('notifyEmpty').hidden = list.length > 0;
    document.getElementById('notifyList').innerHTML = list.length
        ? list.map(n => `
            <div class="notify-item ${n.read ? 'read' : 'unread'}">
                <span>${escapeHtml(n.content)}</span>
                <span class="notify-time">${formatTime(n.createTime)}</span>
            </div>`).join('')
        : '';
}

async function loadUnreadCount() {
    if (!state.user) {
        document.getElementById('unreadBadge').hidden = true;
        return;
    }
    const res = await api('/api/notifications/unread-count');
    if (res.code !== 200) return;
    const count = res.data || 0;
    const badge = document.getElementById('unreadBadge');
    badge.textContent = count > 99 ? '99+' : count;
    badge.hidden = count === 0;
}

async function markAllRead() {
    const res = await api('/api/notifications/read-all', { method: 'PUT' });
    if (res.code !== 200) {
        toast(res.message || '操作失败', 'error');
        return;
    }
    toast('已全部标记为已读', 'success');
    loadNotifications();
    loadUnreadCount();
}

// ==========================================================
//  初始化
// ==========================================================

(async function init() {
    await loadCategories();
    await refreshLoginState();
    await loadProducts(1);
    await loadUnreadCount();
})();
