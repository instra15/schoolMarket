#!/usr/bin/env bash
# ============================================================
# 并发压测：验证乐观锁确实防住了超卖
#
# 原理：
#   1. 用一个库存为 1 的商品作为目标
#   2. 20 个用户同时抢（下单）
#   3. 断言：成功下单数 <= 1，且数据库最终 stock >= 0
#
# 若乐观锁失效，20 个请求会全部读到 stock=1，
# 各自算 stock-1=0 并写回，数据库里会出现多条订单而库存只扣 1 次 —— 即超卖。
#
# 用法：
#   bash scripts/stock-race-test.sh
#
# 前置：应用已启动，MySQL 可用（需要 MYSQL_PWD 或交互式密码）
# ============================================================

set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MYSQL_BIN="${MYSQL_BIN:-/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql}"
DB_USER="${DB_USER:-root}"
DB_NAME="${DB_NAME:-school_market}"
THREADS="${THREADS:-20}"

# 目标商品：库存为 1，便于观察并发争抢
TEST_PRODUCT_ID="${TEST_PRODUCT_ID:-}"
SELLER_ID="${SELLER_ID:-1}"
BUYER_START="${BUYER_START:-100}"

echo "========================================================"
echo " 并发防超卖压测"
echo "   目标 : $BASE_URL"
echo "   并发 : $THREADS"
echo "========================================================"

# ---------- 步骤 0：准备测试商品 ----------
echo ""
echo "[0/5] 准备测试商品（库存=1）..."
if [ -z "$TEST_PRODUCT_ID" ]; then
  TEST_PRODUCT_ID=$("$MYSQL_BIN" -u "$DB_USER" -p"${DATABASE_PASSWORD:-}" -N -B "$DB_NAME" -e \
    "INSERT INTO product (title,description,image_url,price,stock,status,category_id,seller_id,version,create_time,update_time)
     VALUES ('并发测试商品-RUN',NULL,NULL,1.00,1,'ON_SALE',1,$SELLER_ID,0,NOW(),NOW());
     SELECT LAST_INSERT_ID();" 2>/dev/null | tail -1)
  echo "  新建测试商品 ID = $TEST_PRODUCT_ID"
else
  "$MYSQL_BIN" -u "$DB_USER" -p"${DATABASE_PASSWORD:-}" "$DB_NAME" -e \
    "UPDATE product SET stock=1, status='ON_SALE', version=0 WHERE id=$TEST_PRODUCT_ID;" 2>/dev/null
  echo "  重置商品 ID = $TEST_PRODUCT_ID 的库存为 1"
fi

# ---------- 步骤 1：准备多个买家账号 ----------
echo ""
echo "[1/5] 准备 $THREADS 个买家账号..."
COOKIE_DIR=$(mktemp -d)
for i in $(seq 1 $THREADS); do
  UNAME="raceuser$i"
  curl -s -X POST "$BASE_URL/api/users/register" -H "Content-Type: application/json" \
    -d "{\"username\":\"$UNAME\",\"password\":\"123456\",\"phone\":\"139$(printf '%08d' $((13900000000 + i)))\",\"nickname\":\"抢购$i\"}" \
    -o /dev/null
  curl -s -c "$COOKIE_DIR/$UNAME.txt" -X POST "$BASE_URL/api/users/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$UNAME\",\"password\":\"123456\"}" -o /dev/null
done
echo "  完成，Cookie 存于 $COOKIE_DIR"

# ---------- 步骤 2：并发下单 ----------
echo ""
echo "[2/5] 发起 $THREADS 个并发下单请求..."
RESULT_DIR=$(mktemp -d)
for i in $(seq 1 $THREADS); do
  UNAME="raceuser$i"
  (
    HTTP_CODE=$(curl -s -o "$RESULT_DIR/$i.json" -w "%{http_code}" \
      -b "$COOKIE_DIR/$UNAME.txt" \
      -X POST "$BASE_URL/api/orders" \
      -H "Content-Type: application/json" \
      -d "{\"items\":[{\"productId\":$TEST_PRODUCT_ID,\"quantity\":1}]}")
    echo "$HTTP_CODE" > "$RESULT_DIR/$i.code"
  ) &
done
wait
echo "  请求完成"

# ---------- 步骤 3：统计结果 ----------
echo ""
echo "[3/5] 统计结果..."
SUCCESS=0
CONFLICT=0
OTHER=0
for f in "$RESULT_DIR"/*.code; do
  case "$(cat "$f")" in
    200|201) SUCCESS=$((SUCCESS+1)) ;;
    409)     CONFLICT=$((CONFLICT+1)) ;;
    *)       OTHER=$((OTHER+1)) ;;
  esac
done
echo "  HTTP 200 成功 : $SUCCESS"
echo "  HTTP 409 冲突 : $CONFLICT"
echo "  其他          : $OTHER"

# ---------- 步骤 4：核对数据库 ----------
echo ""
echo "[4/5] 核对数据库..."
FINAL_STOCK=$("$MYSQL_BIN" -u "$DB_USER" -p"${DATABASE_PASSWORD:-}" -N -B "$DB_NAME" -e \
  "SELECT stock FROM product WHERE id=$TEST_PRODUCT_ID;" 2>/dev/null | tr -d '\r')
ORDER_COUNT=$("$MYSQL_BIN" -u "$DB_USER" -p"${DATABASE_PASSWORD:-}" -N -B "$DB_NAME" -e \
  "SELECT COUNT(*) FROM order_item oi JOIN \`order\` o ON o.id=oi.order_id
   WHERE oi.product_id=$TEST_PRODUCT_ID;" 2>/dev/null | tr -d '\r')
FINAL_VERSION=$("$MYSQL_BIN" -u "$DB_USER" -p"${DATABASE_PASSWORD:-}" -N -B "$DB_NAME" -e \
  "SELECT version FROM product WHERE id=$TEST_PRODUCT_ID;" 2>/dev/null | tr -d '\r')

echo "  商品最终库存 : $FINAL_STOCK"
echo "  商品最终version: $FINAL_VERSION"
echo "  订单项条数    : $ORDER_COUNT"

# ---------- 步骤 5：断言 ----------
echo ""
echo "[5/5] 断言..."
FAIL=0
if [ "$FINAL_STOCK" -lt 0 ] 2>/dev/null; then
  echo "  [FAIL] 库存为负 ($FINAL_STOCK) —— 发生超卖！"
  FAIL=1
else
  echo "  [PASS] 库存未变负 ($FINAL_STOCK >= 0)"
fi

if [ "$ORDER_COUNT" -gt 1 ] 2>/dev/null; then
  echo "  [FAIL] $ORDER_COUNT 条订单抢到了库存 1 的商品 —— 超卖！"
  FAIL=1
else
  echo "  [PASS] 订单数 ($ORDER_COUNT) 未超过库存"
fi

if [ "$ORDER_COUNT" -ne "$FINAL_VERSION" ] 2>/dev/null; then
  echo "  [WARN] 订单数($ORDER_COUNT) 与 version($FINAL_VERSION) 不一致 —— 排查是否有异常回滚"
fi

echo ""
echo "========================================================"
if [ "$FAIL" -eq 0 ]; then
  echo " 结果: 通过 —— 乐观锁有效，未发生超卖"
else
  echo " 结果: 失败 —— 存在超卖"
fi
echo "========================================================"

# ---------- 清理 ----------
echo ""
read -r -p "是否清理测试数据？(y/N) " ans
if [ "${ans:-N}" = "y" ] || [ "${ans:-N}" = "Y" ]; then
  "$MYSQL_BIN" -u "$DB_USER" -p"${DATABASE_PASSWORD:-}" "$DB_NAME" -e \
    "DELETE oi FROM order_item oi JOIN \`order\` o ON o.id=oi.order_id WHERE oi.product_id=$TEST_PRODUCT_ID;
     DELETE FROM \`order\` WHERE id NOT IN (SELECT DISTINCT order_id FROM order_item);
     DELETE FROM product WHERE id=$TEST_PRODUCT_ID;
     DELETE FROM \`user\` WHERE username LIKE 'raceuser%';" 2>/dev/null
  echo "已清理"
fi
rm -rf "$COOKIE_DIR" "$RESULT_DIR"

exit $FAIL