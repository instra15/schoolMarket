package com.schoolMarket.mapper;

import com.schoolMarket.entity.Notification;
import com.schoolMarket.entity.User;
import com.schoolMarket.support.RequiresDatabase;
import com.schoolMarket.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationMapper 单元测试。
 *
 * <p><b>本测试类的核心价值</b>：验证 {@code is_read -> read} 的显式映射真的生效。
 * 这是 Phase 1 发现的静默失效陷阱 —— 若映射写错，字段会永远是 false
 * 且<b>不报任何错</b>，只有测试能发现。</p>
 */
@RequiresDatabase
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestDataFactory.class)
@ActiveProfiles("test")
@DisplayName("NotificationMapper 单元测试")
class NotificationMapperTest {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private TestDataFactory factory;

    private Notification createNotification(User user, String content, boolean read) {
        Notification n = new Notification();
        n.setUserId(user.getId());
        n.setContent(content);
        n.setRead(read);
        n.setCreateTime(new Date());
        notificationMapper.insert(n);
        return n;
    }

    @Test
    @DisplayName("is_read -> read 映射生效：已读状态能正确读回")
    void readFlagMapping() {
        User user = factory.createUser("nt_read_" + System.nanoTime(), "通知用户");
        Notification n = createNotification(user, "已读消息", true);

        List<Notification> list = notificationMapper.findPageByUser(
                user.getId(), null, 0, 10);

        Notification found = list.stream()
                .filter(x -> x.getId().equals(n.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未查到刚插入的通知"));

        // 若映射失效，这里会是 false —— 测试直接暴露问题
        assertTrue(found.getRead(), "is_read -> read 映射失效：已读通知读成了未读");
    }

    @Test
    @DisplayName("is_read -> read 映射生效：未读状态能正确读回")
    void unreadFlagMapping() {
        User user = factory.createUser("nt_unread_" + System.nanoTime(), "通知用户");
        Notification n = createNotification(user, "未读消息", false);

        List<Notification> list = notificationMapper.findPageByUser(
                user.getId(), null, 0, 10);

        Notification found = list.stream()
                .filter(x -> x.getId().equals(n.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未查到刚插入的通知"));

        assertFalse(found.getRead(), "is_read -> read 映射失效：未读通知读成了已读");
    }

    @Test
    @DisplayName("按已读状态筛选")
    void filterByReadStatus() {
        User user = factory.createUser("nt_filter_" + System.nanoTime(), "通知用户");
        createNotification(user, "消息1", true);
        createNotification(user, "消息2", true);
        createNotification(user, "消息3", false);

        List<Notification> readList = notificationMapper.findPageByUser(user.getId(), true, 0, 10);
        assertEquals(2, readList.size());
        assertTrue(readList.stream().allMatch(Notification::getRead));

        List<Notification> unreadList = notificationMapper.findPageByUser(user.getId(), false, 0, 10);
        assertEquals(1, unreadList.size());
        assertFalse(unreadList.get(0).getRead());

        assertEquals(2, notificationMapper.countByUser(user.getId(), true));
        assertEquals(1, notificationMapper.countByUser(user.getId(), false));
        assertEquals(3, notificationMapper.countByUser(user.getId(), null));
    }

    @Test
    @DisplayName("统计未读数量")
    void countUnread() {
        User user = factory.createUser("nt_count_" + System.nanoTime(), "通知用户");
        createNotification(user, "消息1", false);
        createNotification(user, "消息2", false);
        createNotification(user, "消息3", true);

        assertEquals(2, notificationMapper.countUnread(user.getId()));
    }

    @Test
    @DisplayName("标记已读：幂等，重复标记影响 0 行")
    void markReadIsIdempotent() {
        User user = factory.createUser("nt_mark_" + System.nanoTime(), "通知用户");
        Notification n = createNotification(user, "待读消息", false);

        int first = notificationMapper.markRead(n.getId(), user.getId());
        assertEquals(1, first, "首次标记应影响 1 行");

        int second = notificationMapper.markRead(n.getId(), user.getId());
        assertEquals(0, second, "重复标记应影响 0 行（幂等）");

        List<Notification> list = notificationMapper.findPageByUser(user.getId(), true, 0, 10);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("标记已读：不能操作他人的通知")
    void markReadOthersNotification() {
        User owner = factory.createUser("nt_owner_" + System.nanoTime(), "拥有者");
        User other = factory.createUser("nt_other_" + System.nanoTime(), "他人");
        Notification n = createNotification(owner, "私有消息", false);

        int rows = notificationMapper.markRead(n.getId(), other.getId());
        assertEquals(0, rows, "不应能标记他人的通知");

        assertEquals(1, notificationMapper.countUnread(owner.getId()));
    }

    @Test
    @DisplayName("全部标记已读")
    void markAllRead() {
        User user = factory.createUser("nt_all_" + System.nanoTime(), "通知用户");
        createNotification(user, "消息1", false);
        createNotification(user, "消息2", false);

        int rows = notificationMapper.markAllRead(user.getId());
        assertEquals(2, rows);
        assertEquals(0, notificationMapper.countUnread(user.getId()));
    }
}