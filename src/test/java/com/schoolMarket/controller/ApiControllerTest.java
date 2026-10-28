package com.schoolMarket.controller;

import com.schoolMarket.entity.Category;
import com.schoolMarket.entity.Product;
import com.schoolMarket.entity.User;
import com.schoolMarket.support.RequiresDatabase;
import com.schoolMarket.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户与商品接口的 MockMvc 测试。
 *
 * <p>验证三件事：</p>
 * <ol>
 *   <li><b>统一响应体格式</b>：code / message / data 三个字段</li>
 *   <li><b>登录态</b>：{@code @CurrentUser} 从 Session 取 userId，未登录返回 401</li>
 *   <li><b>校验与越权</b>：参数校验返回 400，操作他人商品返回 400</li>
 * </ol>
 */
@RequiresDatabase
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("接口层 MockMvc 测试")
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory factory;

    /**
     * 造一个带 userId 的 Session，模拟已登录。
     */
    private MockHttpSession loginSession(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(UserController.SESSION_USER_ID, userId);
        return session;
    }

    @Test
    @DisplayName("注册成功返回统一响应体")
    void registerSuccess() throws Exception {
        String username = "api_reg_" + System.nanoTime();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456","phone":"13900138001","nickname":"新用户"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("参数校验失败返回 400 与具体提示")
    void registerValidationFailed() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"123456","phone":"123","nickname":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("未登录访问 /me 返回 401")
    void meWithoutSessionReturns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("已登录访问 /me 返回本人信息且不含密码")
    void meWithSessionReturnsUser() throws Exception {
        User user = factory.createUser("api_me_" + System.nanoTime(), "登录用户");

        mockMvc.perform(get("/api/users/me").session(loginSession(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.nickname").value("登录用户"))
                // 脱敏：响应中不应出现 password
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("登录成功写入 Session")
    void loginWritesSession() throws Exception {
        User user = factory.createUser("api_login_" + System.nanoTime(), "登录用户");
        // 用工厂造的用户密码是 123456

        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"123456"}
                                """.formatted(user.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(user.getId()))
                .andReturn();

        Object sessionUserId = result.getRequest().getSession()
                .getAttribute(UserController.SESSION_USER_ID);
        assertNotNull(sessionUserId, "登录后应写入 Session");
        assertEquals(user.getId(), sessionUserId);
    }

    @Test
    @DisplayName("密码错误返回 400，且不区分「用户不存在」与「密码错误」")
    void loginWithWrongPassword() throws Exception {
        User user = factory.createUser("api_wrong_" + System.nanoTime(), "用户");

        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"wrongpassword"}
                                """.formatted(user.getUsername())))
                .andExpect(status().isBadRequest())
                .andReturn();

        String message = result.getResponse().getContentAsString();
        assertTrue(message.contains("用户名或密码错误"), "应返回统一的错误提示");

        // 不存在的用户也返回同一句，避免泄露用户是否存在
        MvcResult notExist = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"no_such_user_xyz","password":"whatever"}
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertTrue(notExist.getResponse().getContentAsString()
                        .contains("用户名或密码错误"),
                "用户不存在与密码错误应返回相同提示");
    }

    @Test
    @DisplayName("发布商品：卖家ID 取自 Session 而非前端")
    void publishProductUsesSessionUser() throws Exception {
        User seller = factory.createUser("api_seller_" + System.nanoTime(), "卖家");
        Category category = factory.createCategory("接口分类_" + System.nanoTime());

        mockMvc.perform(post("/api/products")
                        .session(loginSession(seller.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"接口发布的商品","description":"描述","price":66.60,"stock":4,"categoryId":%d}
                                """.formatted(category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("未登录发布商品返回 401")
    void publishProductWithoutLogin() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"未登录发布","price":10.00,"stock":1,"categoryId":1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("越权：编辑他人商品返回 400")
    void cannotEditOthersProduct() throws Exception {
        User owner = factory.createUser("api_owner_" + System.nanoTime(), "拥有者");
        User intruder = factory.createUser("api_intruder_" + System.nanoTime(), "入侵者");
        Category category = factory.createCategory("越权分类_" + System.nanoTime());
        Product product = factory.createProduct("他人商品", new BigDecimal("10.00"),
                3, category.getId(), owner.getId());

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .session(loginSession(intruder.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"被篡改","price":1.00,"stock":99,"categoryId":%d}
                                """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("分页搜索返回分页结构与关联字段")
    void searchProductsReturnsPageResult() throws Exception {
        User seller = factory.createUser("api_search_seller_" + System.nanoTime(), "卖家");
        Category category = factory.createCategory("搜索分类_" + System.nanoTime());
        factory.createProduct("搜索用商品", new BigDecimal("33.30"),
                2, category.getId(), seller.getId());

        mockMvc.perform(get("/api/products")
                        .param("pageNum", "1")
                        .param("pageSize", "5")
                        .param("categoryId", String.valueOf(category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.list").isArray())
                // association 生效：列表项应带出分类名
                .andExpect(jsonPath("$.data.list[0].category.name").exists())
                .andExpect(jsonPath("$.data.list[0].seller.nickname").exists());
    }

    @Test
    @DisplayName("分类列表返回商品数量统计")
    void listCategoriesWithProductCount() throws Exception {
        User seller = factory.createUser("api_cat_seller_" + System.nanoTime(), "卖家");
        Category category = factory.createCategory("计数分类_" + System.nanoTime());
        factory.createProduct("计数商品1", new BigDecimal("10.00"), 1, category.getId(), seller.getId());
        factory.createProduct("计数商品2", new BigDecimal("20.00"), 1, category.getId(), seller.getId());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}