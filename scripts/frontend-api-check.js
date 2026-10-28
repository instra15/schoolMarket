/**
 * 前端 API 契约回归测试
 *
 * 用 Node 直接按 app.js 的调用顺序打一遍后端，验证：
 *   1. 前端依赖的每个字段都真实存在（字段名对不上是前端最常见的坑）
 *   2. Session Cookie 机制正常（登录 → 带 Cookie 访问 → 拿到身份）
 *   3. 动态特性正常（异步通知写入、支付幂等、动态条件筛选）
 *
 * 用法：
 *   node scripts/frontend-api-check.js [baseUrl]
 *   node scripts/frontend-api-check.js http://localhost:8080
 *
 * 注意：脚本用 lisi 账号登录，因此只能买「卖家不是 lisi」的商品。
 * 默认买 productId=3（种子数据里卖家是 zhangsan）。
 */
const BASE = process.argv[2] || 'http://localhost:8080';
let cookie = '';

async function api(url, options = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (cookie) headers['Cookie'] = cookie;
    const config = { method: options.method || 'GET', headers, ...options };
    if (config.body) config.body = JSON.stringify(config.body);
    const resp = await fetch(BASE + url, config);
    const setCookie = resp.headers.get('set-cookie');
    if (setCookie) cookie = setCookie.split(';')[0];
    return await resp.json();
}

function check(name, cond, extra) {
    console.log(`${cond ? '[PASS]' : '[FAIL]'} ${name}${extra ? ' → ' + extra : ''}`);
    return cond;
}

(async () => {
    // 1. 分类（init 阶段）
    const cats = await api('/api/categories');
    check('GET /api/categories 返回数组', Array.isArray(cats.data) && cats.data.length > 0,
          cats.data?.length + ' 个分类');
    check('分类含 productCount（前端渲染用）',
          cats.data.every(c => 'productCount' in c));

    // 2. 登录态探针（未登录）
    let me = await api('/api/users/me');
    check('未登录 /api/users/me → 401', me.code === 401, 'code=' + me.code);

    // 3. 商品列表（前端 loadProducts 用到的字段）
    const products = await api('/api/products?pageNum=1&pageSize=3');
    check('GET /api/products 返回分页结构',
          'total' in products.data && 'list' in products.data && 'pageNum' in products.data);
    const p = products.data.list[0];
    check('商品含 category.name（association）', p?.category?.name);
    check('商品含 seller.nickname（association）', p?.seller?.nickname);
    check('商品含 price/stock/status', p && 'price' in p && 'stock' in p && 'status' in p);

    // 4. 登录
    const login = await api('/api/users/login', {
        method: 'POST', body: { username: 'lisi', password: '123456' } });
    check('POST /api/users/login → 200', login.code === 200, 'userId=' + login.data);
    check('拿到 Session Cookie', cookie.startsWith('JSESSIONID='), cookie.slice(0, 30));

    // 5. 登录态探针（已登录）
    me = await api('/api/users/me');
    check('已登录 /api/users/me → 200', me.code === 200, me.data?.nickname);
    check('用户信息不含 password', me.data && !('password' in me.data));

    // 6. 通知
    const unread = await api('/api/notifications/unread-count');
    check('GET /api/notifications/unread-count 返回数字', typeof unread.data === 'number',
          'unread=' + unread.data);

    // 7. 订单列表
    const orders = await api('/api/orders?pageNum=1&pageSize=5');
    check('GET /api/orders 返回分页', orders.code === 200 && 'list' in orders.data);
    if (orders.data.list.length) {
        const o = orders.data.list[0];
        check('订单含 orderNo/status/totalPrice',
              o.orderNo && o.status && 'totalPrice' in o);
        check('订单含 items 数组（collection）', Array.isArray(o.items));
        if (o.items?.length) {
            check('订单项含 productTitle/quantity/price',
                  'productTitle' in o.items[0] && 'quantity' in o.items[0] && 'price' in o.items[0]);
        }
    }

    // 8. 我卖出的
    const sold = await api('/api/orders/sold?pageNum=1&pageSize=5');
    check('GET /api/orders/sold → 200', sold.code === 200);

    // 9. 下单
    // 脚本自给自足：先以 lisi 身份发布一件商品（卖家是 zhangsan 时买不了自己的），
    // 再让 zhangsan 来买这件商品。这样不依赖种子数据的库存状态，可重复运行。
    const publishRes = await api('/api/products', {
        method: 'POST',
        body: {
            title: '前端契约测试商品-' + Date.now(),
            description: '由 frontend-api-check.js 自动创建',
            price: 9.9,
            stock: 10,
            categoryId: (cats.data && cats.data[0] ? cats.data[0].id : 1)
        }
    });
    check('POST /api/products → 200（发布测试商品）', publishRes.code === 200,
          'productId=' + publishRes.data);

    // 换成 zhangsan 来买 lisi 发布的商品
    const login2 = await api('/api/users/login', {
        method: 'POST', body: { username: 'zhangsan', password: '123456' } });
    check('切换账号登录 → 200', login2.code === 200);

    let buy = null;
    let lastError = '';
    if (publishRes.code === 200) {
        const r = await api('/api/orders', {
            method: 'POST', body: { items: [{ productId: publishRes.data, quantity: 1 }] } });
        if (r.code === 200) {
            buy = r;
        } else {
            lastError = r.message || ('code=' + r.code);
        }
    }
    check('POST /api/orders → 200', buy !== null,
          buy ? 'orderNo=' + buy.data : '失败原因: ' + lastError);

    // 10. 支付
    if (buy) {
        const pay = await api('/api/orders/' + buy.data + '/pay', { method: 'PUT' });
        check('PUT /api/orders/{no}/pay → 200', pay.code === 200);
        const pay2 = await api('/api/orders/' + buy.data + '/pay', { method: 'PUT' });
        check('重复支付仍 200（幂等）', pay2.code === 200);
    }

    // 11. 通知异步写入（稍等）
    await new Promise(r => setTimeout(r, 1200));
    const notif = await api('/api/notifications?pageNum=1&pageSize=10');
    check('下单后通知已异步写入', notif.data?.list?.length > 0,
          (notif.data?.list?.[0]?.content || '').slice(0, 40));

    // 12. 分类筛选
    const filtered = await api('/api/products?categoryId=1&pageNum=1&pageSize=5');
    check('GET /api/products?categoryId 过滤生效',
          filtered.data.list.every(x => x.category?.id === 1), filtered.data.list.length + ' 件');

    // 13. 关键词搜索
    const kw = await api('/api/products?keyword=' + encodeURIComponent('教材'));
    check('GET /api/products?keyword 搜索生效', kw.code === 200, kw.data.total + ' 条');

    // 14. 退出
    const logout = await api('/api/users/logout', { method: 'POST' });
    check('POST /api/users/logout → 200', logout.code === 200);
})();
