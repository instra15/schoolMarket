package com.schoolMarket.service.impl;

import com.schoolMarket.common.PageResult;
import com.schoolMarket.dto.PageQueryDTO;
import com.schoolMarket.entity.Notification;
import com.schoolMarket.mapper.NotificationMapper;
import com.schoolMarket.service.NotificationService;
import com.schoolMarket.util.TransactionAfterCommitExecutor;
import com.schoolMarket.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    @Override
    public void asyncNotify(Long userId, String content) {
        if (userId == null || content == null || content.isBlank()) {
            return;
        }

        // 挂到 afterCommit 上：事务提交成功后才真正异步写库。
        // 这样既不会拖累主流程，也不会出现「事务回滚了却发了通知」。
        afterCommitExecutor.executeAfterCommit(() -> {
            try {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setContent(content);
                notification.setRead(false);
                notification.setCreateTime(new Date());
                notificationMapper.insert(notification);
                log.debug("异步通知已写入 userId={} content={}", userId, content);
            } catch (Exception e) {
                // 兜底：通知写失败绝不能冒泡影响调用方（虽然已在独立线程）
                log.error("写通知失败 userId={} content={}", userId, content, e);
            }
        });
    }

    @Override
    public PageResult<NotificationVO> pageMine(Long userId, PageQueryDTO query, Boolean read) {
        long total = notificationMapper.countByUser(userId, read);
        List<Notification> list = total == 0 ? List.of()
                : notificationMapper.findPageByUser(userId, read, query.getOffset(), query.getPageSize());

        List<NotificationVO> voList = list.stream().map(this::toVO).toList();
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), voList);
    }

    @Override
    public int countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long notificationId) {
        // SQL 里带了 user_id 条件，天然防止改别人的通知。
        // 影响 0 行 = 不存在 或 已读，统一提示「已读或不存在」，不泄露他人数据是否存在。
        int rows = notificationMapper.markRead(notificationId, userId);
        if (rows == 0) {
            log.debug("标记已读未生效 notificationId={} userId={}（可能已读或不属于该用户）",
                    notificationId, userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long userId) {
        return notificationMapper.markAllRead(userId);
    }

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        vo.setId(n.getId());
        vo.setContent(n.getContent());
        vo.setRead(n.getRead());
        vo.setCreateTime(n.getCreateTime());
        return vo;
    }
}