package com.schoolMarket.concurrent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发下单压测（纯 JDK 版，不依赖 JMeter）。
 *
 * <p>用法：</p>
 * <pre>
 * # 1. 启动应用
 * mvn spring-boot:run
 *
 * # 2. 准备一个库存为 1 的商品，记下它的ID
 *    UPDATE product SET stock=1, status='ON_SALE' WHERE id=1;
 *
 * # 3. 运行压测（两个参数：并发数、商品ID）
 * mvn compile
 * java -cp target/classes;$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout) \
 *      com.schoolMarket.concurrent.StockRaceTest 20 1
 *
 * # 或者在 IDE 里直接 Run，并设置 Program arguments 为 "20 1"
 * </pre>
 *
 * <p>相比 shell 版 curl 并发的优势：用 {@link CountDownLatch} 让所有线程在
 * <b>同一瞬间</b>起跑，而 shell 的后台 {@code &} 是错开的，真实争抢压力大得多。</p>
 *
 * <p><b>判定标准</b>：若商品库存为 1，而本程序报告的「成功下单数」&gt; 1，
 * 则说明发生了超卖，乐观锁失效。</p>
 */
public class StockRaceTest {

    private static String baseUrl = "http://localhost:8080";
    private static int threads = 20;
    private static long productId = 0L;

    private static final AtomicInteger success = new AtomicInteger();
    private static final AtomicInteger conflict = new AtomicInteger();
    private static final AtomicInteger other = new AtomicInteger();

    /** 本次压测创建的所有账号，便于事后清理 */
    private static final List<String> createdUsers =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            threads = Integer.parseInt(args[0].trim());
        }
        if (args.length > 1) {
            productId = Long.parseLong(args[1].trim());
        }

        System.out.println("================================================");
        System.out.println(" 并发下单压测（乐观锁防超卖验证）");
        System.out.println("   目标: " + baseUrl);
        System.out.println("   并发: " + threads);
        System.out.println("================================================");

        if (productId <= 0) {
            System.err.println("用法: StockRaceTest <并发数> <商品ID>");
            System.err.println("示例: StockRaceTest 20 1");
            System.exit(2);
        }

        // ---------- 1. 每个线程准备一个独立账号并登录 ----------
        System.out.println("\n[1/3] 准备 " + threads + " 个测试账号...");
        String[] cookies = new String[threads];
        String batch = String.valueOf(System.currentTimeMillis() % 100000);

        for (int i = 0; i < threads; i++) {
            String username = "racetest" + batch + "_" + i;
            String phone = "138" + String.format("%08d", (int) (System.nanoTime() % 99_999_999) + i);

            register(username, phone);
            cookies[i] = login(username);
            if (cookies[i] == null) {
                System.err.println("  登录失败: " + username);
                System.exit(3);
            }
            createdUsers.add(username);
        }
        System.out.println("  完成");

        // ---------- 2. CountDownLatch 让所有线程同时起跑 ----------
        System.out.println("\n[2/3] 并发下单（同一瞬间放行）...");
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final String cookie = cookies[i];
            pool.submit(() -> {
                ready.countDown();
                try {
                    // 阻塞等待统一放行，确保所有线程尽可能同时冲
                    start.await();
                    String body = "{\"items\":[{\"productId\":" + productId + ",\"quantity\":1}]}";
                    int code = postOrder(cookie, body);
                    if (code == 200 || code == 201) {
                        success.incrementAndGet();
                    } else if (code == 409) {
                        conflict.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                        if (other.get() <= 3) {
                            System.out.println("  [其他错误] HTTP " + code);
                        }
                    }
                } catch (Exception e) {
                    other.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(30, TimeUnit.SECONDS);   // 等所有线程就位
        start.countDown();                  // 同时放行
        done.await(120, TimeUnit.SECONDS);  // 等全部结束
        pool.shutdown();
        System.out.println("  请求完成");

        // ---------- 3. 输出结果 ----------
        System.out.println("\n[3/3] 结果统计");
        System.out.println("  成功下单(200/201) : " + success.get());
        System.out.println("  库存冲突(409)     : " + conflict.get());
        System.out.println("  其他错误          : " + other.get());

        System.out.println("\n请在数据库核对最终状态：");
        System.out.println("  SELECT stock, version FROM product WHERE id = " + productId + ";");
        System.out.println("  SELECT COUNT(*) FROM order_item WHERE product_id = " + productId + ";");

        if (success.get() > 1) {
            System.err.println("\n!! 警告：成功数 = " + success.get()
                    + " > 1。若该商品库存仅 1 件，说明发生超卖，乐观锁失效！");
            System.exit(1);
        }
        System.out.println("\n若商品库存为 1 且成功数 <= 1，则乐观锁有效，未发生超卖。");
        System.out.println("测试账号前缀: racetest" + batch + "_  （可自行清理）");
    }

    /** 注册测试账号 */
    private static void register(String username, String phone) {
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"123456\",\"phone\":\"%s\",\"nickname\":\"%s\"}",
                username, phone, username);
        send("/api/users/register", body, null);
    }

    /** 登录并返回 Cookie 头值 */
    private static String login(String username) {
        String body = String.format(
                "{\"username\":\"%s\",\"password\":\"123456\"}", username);
        try {
            HttpURLConnection conn = open("/api/users/login", body);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            String cookie = firstCookie(conn);
            drain(conn);
            return code == 200 ? cookie : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 下单并返回 HTTP 状态码 */
    private static int postOrder(String cookie, String body) {
        try {
            HttpURLConnection conn = open("/api/orders", body);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            if (cookie != null) {
                conn.setRequestProperty("Cookie", cookie);
            }
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            drain(conn);
            return code;
        } catch (Exception e) {
            return -1;
        }
    }

    private static void send(String path, String body, String cookie) {
        try {
            HttpURLConnection conn = open(path, body);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            drain(conn);
            conn.disconnect();
        } catch (Exception ignored) {
            // 注册失败不阻断流程，登录时才会暴露
        }
    }

    private static HttpURLConnection open(String path, String body) throws Exception {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);
        return conn;
    }

    /** 取第一个 Cookie 的 name=value 部分 */
    private static String firstCookie(HttpURLConnection conn) {
        List<String> setCookies = conn.getHeaderFields()
                .getOrDefault("Set-Cookie", Collections.emptyList());
        for (String sc : setCookies) {
            String pair = sc.split(";")[0].trim();
            if (pair.startsWith("JSESSIONID=")) {
                return pair;
            }
        }
        return setCookies.isEmpty() ? null
                : setCookies.get(0).split(";")[0].trim();
    }

    /** 读取并丢弃响应体，避免连接未完全消费 */
    private static void drain(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            while (br.readLine() != null) {
                // just drain
            }
        } catch (Exception ignored) {
            // ignore
        }
    }
}