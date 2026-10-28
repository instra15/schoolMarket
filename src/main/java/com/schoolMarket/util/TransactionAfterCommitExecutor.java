package com.schoolMarket.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 「事务提交后执行」执行器 —— 本阶段最关键的类。
 *
 * <p><b>要解决的问题</b>：下单成功要写通知表。如果直接在 {@code @Transactional} 方法里写，
 * 会出问题：</p>
 * <ul>
 *   <li>通知写失败 → 整个下单事务回滚 → 主流程被辅助数据拖累</li>
 *   <li>通知写成功但事务后续回滚 → 通知说「下单成功」而订单实际不存在（脏数据）</li>
 * </ul>
 *
 * <p><b>本类的做法</b>：把任务挂到事务的 afterCommit 回调上，
 * 事务真正提交成功后才提交给线程池执行。</p>
 *
 * <p><b>对比其他做法</b>：</p>
 * <table border="1">
 *   <tr><th>方案</th><th>问题</th></tr>
 *   <tr><td>直接把异步写在事务方法里</td><td>异步可能在事务提交前就执行，读不到未提交的数据；事务回滚了通知也发了</td></tr>
 *   <tr><td>在外层方法（无事务）里调异步</td><td>可行，但要把业务方法拆成两层，代码割裂</td></tr>
 *   <tr><td><b>afterCommit 回调</b></td><td>语义最清晰：事务提交成功才执行，且不必拆分方法</td></tr>
 * </table>
 *
 * <p><b>注意</b>：本项目是「最终一致性」思路 —— 下单事务提交是前提，
 * 异步任务基于已提交的事实执行。通知/日志属辅助数据，极端情况下允许丢失，
 * 不影响核心交易。这正是计划书「难点三」所权衡的点。</p>
 *
 * <p><b>⚠ 两种调用点的语义不同</b>：</p>
 * <ul>
 *   <li><b>事务方法内调用</b>（如 {@code OrderServiceImpl.createOrder} 里发通知）：
 *       会真正挂上 afterCommit 回调，事务回滚则任务被跳过。</li>
 *   <li><b>Controller 切面里调用</b>（如 {@code LogOperationAspect}）：
 *       AOP 后置通知触发时，Service 的事务<b>已经提交完毕</b>，
 *       此时 {@code isSynchronizationActive()} 为 false，走「无事务直接执行」分支。
 *       效果依然正确 —— 业务已成功，只是没有事务可挂靠而已。</li>
 * </ul>
 *
 * <p><b>⚠ 异步线程没有事务上下文</b>：任务运行在全新线程里，
 * 不会继承主线程的事务与数据库连接绑定。后果是：</p>
 * <ul>
 *   <li>任务里的<b>单条</b>写库操作会自动提交，没问题</li>
 *   <li>若任务里需要<b>多条</b>写库且要求原子性，必须在任务内部自己用
 *       {@code TransactionTemplate} 包一层，否则可能出现部分写入</li>
 * </ul>
 * <p>本项目的通知/日志写入都是单条 insert，不涉及这个问题，故保持简单。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionAfterCommitExecutor {

    private final ThreadPoolHolder threadPoolHolder;

    /**
     * 在事务提交后执行任务。
     *
     * <p>若当前不在事务中（如 Controller 直接调用），则立即提交给线程池。</p>
     *
     * @param task 任务逻辑
     */
    public void executeAfterCommit(Runnable task) {
        if (task == null) {
            return;
        }

        // 没有活动事务：直接执行
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("当前无事务上下文，直接提交异步任务");
            threadPoolHolder.submit(task);
            return;
        }

        // 有事务：注册 afterCommit 回调，等事务真正提交后才提交给线程池
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.debug("事务已提交，提交异步任务");
                threadPoolHolder.submit(task);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    // 事务回滚了，任务本就不会被提交（afterCommit 不会触发）。
                    // 这里仅用于排查时定位「为什么没收到通知」
                    log.debug("事务未提交（status={}），异步任务被跳过", status);
                }
            }
        });
    }
}