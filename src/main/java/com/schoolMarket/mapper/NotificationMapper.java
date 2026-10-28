package com.schoolMarket.mapper;

import com.schoolMarket.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 Mapper。通知由下单流程异步写入。
 */
@Mapper
public interface NotificationMapper {

    /**
     * 新增通知。
     */
    int insert(Notification notification);

    /**
     * 分页查询某用户的通知。
     *
     * @param userId 用户ID
     * @param read   是否已读，传 null 则查全部
     */
    List<Notification> findPageByUser(@Param("userId") Long userId,
                                      @Param("read") Boolean read,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    /**
     * 统计某用户的通知总数，{@code read} 传 null 则统计全部。
     */
    long countByUser(@Param("userId") Long userId, @Param("read") Boolean read);

    /**
     * 统计未读数量，用于「未读消息红点」。
     */
    int countUnread(@Param("userId") Long userId);

    /**
     * 标记已读。带 {@code is_read = 0} 条件保证幂等。
     *
     * @return 影响行数
     */
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 全部标记已读，返回影响行数。
     */
    int markAllRead(@Param("userId") Long userId);
}