package com.schoolMarket.service;

import com.schoolMarket.common.PageResult;
import com.schoolMarket.dto.PageQueryDTO;
import com.schoolMarket.vo.NotificationVO;

/**
 * 通知服务。
 *
 * <p>写通知一律走 {@link #asyncNotify} —— 异步执行，不阻塞主流程。</p>
 */
public interface NotificationService {

    /**
     * 异步发送通知。
     *
     * <p>调用方通常在事务方法内调用，本方法会挂到 afterCommit 回调上，
     * 事务真正提交成功后才真正写库。若不在事务中则立即提交给线程池。</p>
     *
     * @param userId 接收人
     * @param content 通知内容
     */
    void asyncNotify(Long userId, String content);

    /**
     * 我的通知分页查询。
     *
     * @param query 分页参数，{@code read} 为 null 查全部
     */
    PageResult<NotificationVO> pageMine(Long userId, PageQueryDTO query, Boolean read);

    /**
     * 未读通知数量，用于消息红点。
     */
    int countUnread(Long userId);

    /**
     * 标记单条已读。只能标记自己的通知。
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 全部标记已读。
     *
     * @return 本次实际标记的条数
     */
    int markAllRead(Long userId);
}