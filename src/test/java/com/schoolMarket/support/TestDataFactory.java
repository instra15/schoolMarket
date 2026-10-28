package com.schoolMarket.support;

import com.schoolMarket.entity.Category;
import com.schoolMarket.entity.Product;
import com.schoolMarket.entity.User;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.mapper.CategoryMapper;
import com.schoolMarket.mapper.ProductMapper;
import com.schoolMarket.mapper.UserMapper;
import com.schoolMarket.util.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 测试数据工厂。
 *
 * <p>各测试类用它造出确定性的前置数据（用户、分类、商品），
 * 避免在每个测试里重复写 insert 语句。</p>
 *
 * <p>只提供「造数据」不提供「清数据」—— 测试库随时可以整体重建
 * （{@code mysql -u root -p < sql/init-test.sql}），逐表清理反而容易漏。</p>
 */
@Component
public class TestDataFactory {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    /**
     * 造一个用户，密码统一为 123456。
     */
    public User createUser(String username, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoder.encode("123456"));
        user.setPhone("139" + String.format("%08d", Math.abs(username.hashCode() % 100_000_000)));
        user.setNickname(nickname);
        user.setCreateTime(new Date());
        userMapper.insert(user);
        return user;
    }

    /**
     * 造一个分类。
     */
    public Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(name + "的描述");
        category.setCreateTime(new Date());
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 造一个上架商品。
     */
    public Product createProduct(String title, BigDecimal price, int stock,
                                 Long categoryId, Long sellerId) {
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(title + "的描述");
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus(ProductStatus.ON_SALE);
        product.setCategoryId(categoryId);
        product.setSellerId(sellerId);
        product.setVersion(0);
        Date now = new Date();
        product.setCreateTime(now);
        product.setUpdateTime(now);
        productMapper.insert(product);
        return product;
    }
}