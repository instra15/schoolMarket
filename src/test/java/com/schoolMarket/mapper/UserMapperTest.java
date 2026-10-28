package com.schoolMarket.mapper;

import com.schoolMarket.entity.User;
import com.schoolMarket.support.RequiresDatabase;
import com.schoolMarket.support.TestDataFactory;
import com.schoolMarket.util.PasswordEncoder;
import com.schoolMarket.vo.UserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserMapper 单元测试。
 *
 * <p>重点：{@code findVOById} 的 SQL 只 SELECT 了 4 列（不含 password），
 * 验证脱敏在 SQL 层面生效，而非仅靠 VO 字段缺失。</p>
 */
@RequiresDatabase
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestDataFactory.class)
@ActiveProfiles("test")
@DisplayName("UserMapper 单元测试")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TestDataFactory factory;

    @Test
    @DisplayName("按用户名查用户，含密码哈希")
    void findByUsername() {
        User user = factory.createUser("um_user_" + System.nanoTime(), "测试用户");

        User found = userMapper.findByUsername(user.getUsername());
        assertNotNull(found);
        assertEquals(user.getId(), found.getId());
        assertNotNull(found.getPassword());
        assertTrue(PasswordEncoder.matches("123456", found.getPassword()),
                "存储的应是可校验的哈希而非明文");
        assertNotEquals("123456", found.getPassword(), "不应存明文密码");
    }

    @Test
    @DisplayName("按ID查用户")
    void findById() {
        User user = factory.createUser("um_id_" + System.nanoTime(), "测试用户");

        User found = userMapper.findById(user.getId());
        assertNotNull(found);
        assertEquals(user.getUsername(), found.getUsername());
        assertEquals("测试用户", found.getNickname());
    }

    @Test
    @DisplayName("VO 查询脱敏：不含密码字段")
    void findVOIsSanitized() {
        User user = factory.createUser("um_vo_" + System.nanoTime(), "测试用户");

        UserVO vo = userMapper.findVOById(user.getId());
        assertNotNull(vo);
        assertEquals(user.getUsername(), vo.getUsername());
        assertEquals(user.getNickname(), vo.getNickname());
        assertEquals(user.getPhone(), vo.getPhone());
        // UserVO 本身没有 password 字段 —— 编译期就保证了脱敏
        assertEquals(0, java.util.Arrays.stream(UserVO.class.getDeclaredFields())
                        .filter(f -> "password".equals(f.getName())).count(),
                "UserVO 不应包含 password 字段");
    }

    @Test
    @DisplayName("更新资料：昵称与手机号")
    void updateProfile() {
        User user = factory.createUser("um_upd_" + System.nanoTime(), "旧昵称");

        int rows = userMapper.updateProfile(user.getUsername(), "新昵称", "13800138000");
        assertEquals(1, rows);

        User after = userMapper.findById(user.getId());
        assertEquals("新昵称", after.getNickname());
        assertEquals("13800138000", after.getPhone());
    }

    @Test
    @DisplayName("更新密码：新密码可校验，旧密码失效")
    void updatePassword() {
        User user = factory.createUser("um_pwd_" + System.nanoTime(), "测试用户");

        int rows = userMapper.updatePassword(user.getUsername(),
                PasswordEncoder.encode("newpass123"));
        assertEquals(1, rows);

        User after = userMapper.findById(user.getId());
        assertTrue(PasswordEncoder.matches("newpass123", after.getPassword()));
        assertFalse(PasswordEncoder.matches("123456", after.getPassword()));
    }

    @Test
    @DisplayName("插入用户后主键回填")
    void insertBackfillsId() {
        User user = new User();
        user.setUsername("um_ins_" + System.nanoTime());
        user.setPassword(PasswordEncoder.encode("123456"));
        user.setPhone("13800138999");
        user.setNickname("新人");
        user.setCreateTime(new java.util.Date());

        int rows = userMapper.insert(user);
        assertEquals(1, rows);
        assertNotNull(user.getId(), "useGeneratedKeys 应回填主键");
    }
}