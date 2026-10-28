package com.schoolMarket.util;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JDK 原生线程池工具类。
 *
 * <p><b>刻意不使用 {@code @Async}</b> —— 计划书明确要求不引入超纲内容，
 * 所有并发能力基于 {@code ExecutorService} + 数据库乐观锁实现。</p>
 *
 * <p><b>为什么用有界队列而非 {@code Executors.newFixedThreadPool}</b>：
 * newFixedThreadPool 用的是无界 LinkedBlockingQueue，队列满了会一直堆积直到 OOM。
 * 这里用 {@link ArrayBlockingQueue} 有界容量 + 明确的拒绝策略，
 * 队列满时快速失败（调用方可感知），而不是无声地堆积内存。</p>
 *
 * <p>核心参数遵循《Java 并发编程实战》线程池实践：</p>
 * <ul>
 *   <li>核心线程数 = CPU 核数 + 1</li>
 *   <li>最大线程数 = CPU 核数 × 2（本项目是 IO 密集型：写库+通知）</li>
 *   <li>队列容量适中，避免任务在内存里长时间滞留</li>
 *   <li>拒绝策略 CallerRunsPolicy：队列满时由提交线程自己执行，
 *       形成天然背压，且不会丢任务</li>
 * </ul>
 */
@Slf4j
@Component
public class ThreadPoolHolder {

    @Value("${app.thread-pool.core-size:0}")
    private int configuredCoreSize;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int coreSize = configuredCoreSize > 0 ? configuredCoreSize : cpuCores + 1;
        int maxSize = Math.max(coreSize * 2, cpuCores * 2);

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(256),
                new NamedThreadFactory(),
                // 队列满时由调用线程执行，形成背压而不是丢任务
                new ThreadPoolExecutor.CallerRunsPolicy());

        pool.allowCoreThreadTimeOut(false);
        this.executorService = pool;

        log.info("线程池初始化完成：核心线程={} 最大线程={} 队列容量={}",
                coreSize, maxSize, 256);
    }

    /**
     * 提交一个异步任务。
     *
     * <p>任务内的异常已在 {@link SafeRunnable} 里捕获并记录，
     * 不会因为任务抛异常而导致调用方收到异常 —— 异步任务失败不该影响主流程。</p>
     */
    public void submit(Runnable task) {
        if (executorService == null || executorService.isShutdown()) {
            log.warn("线程池已关闭，任务被丢弃");
            return;
        }
        try {
            executorService.execute(new SafeRunnable(task));
        } catch (RejectedExecutionException e) {
            log.warn("任务被线程池拒绝：{}", e.getMessage());
        }
    }

    /**
     * 批量提交任务。
     */
    public void submitAll(List<Runnable> tasks) {
        if (tasks == null) {
            return;
        }
        tasks.forEach(this::submit);
    }

    /**
     * 优雅关闭：先停止接收新任务，再等待已有任务完成，超时则强制中断。
     *
     * <p>由 {@link PreDestroy} 在容器关闭时自动调用，
     * 避免线程泄漏导致 JVM 无法退出。</p>
     */
    @PreDestroy
    public void shutdown() {
        if (executorService == null) {
            return;
        }
        log.info("线程池开始关闭...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("任务未在10秒内完成，强制关闭");
                executorService.shutdownNow();
            }
            log.info("线程池已关闭");
        } catch (InterruptedException e) {
            log.warn("关闭线程池时被中断");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 当前线程池状态，供监控/测试用。
     */
    public boolean isRunning() {
        return executorService != null && !executorService.isShutdown();
    }

    /**
     * 包装任务，吞掉所有异常。
     *
     * <p>不这样做的话，任务里的 RuntimeException 会被 Thread 的默认处理器
     * 打到 stderr，而主线程完全不知情 —— 排查问题时非常难找。</p>
     */
    private static class SafeRunnable implements Runnable {
        private final Runnable delegate;

        SafeRunnable(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            try {
                delegate.run();
            } catch (Exception e) {
                log.error("异步任务执行失败", e);
            }
        }
    }

    /**
     * 自定义线程工厂，给线程起可读名字。
     *
     * <p>线程名带序号后，线上 dump 线程栈时能立刻看出是哪个池的线程。</p>
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "school-market-async-" + index.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}