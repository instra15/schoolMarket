-- ============================================================
-- 校园二手交易平台 —— 测试库初始化脚本
--
-- 与 sql/init.sql 结构完全一致，仅库名不同、且不预置种子数据。
-- 测试之间会互相影响数据，因此测试库可随时重建：
--   mysql -u root -p < sql/init-test.sql
-- ============================================================

DROP DATABASE IF EXISTS school_market_test;
CREATE DATABASE school_market_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE school_market_test;


-- ---------- 用户表 ----------
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(50)  NOT NULL COMMENT '用户名，登录账号',
  `password`    VARCHAR(64)  NOT NULL COMMENT '密码哈希（SHA-256 加盐，勿存明文）',
  `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号',
  `nickname`    VARCHAR(50)  NOT NULL COMMENT '昵称，对外展示',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_phone` (`phone`)
) ENGINE=InnoDB COMMENT='用户表';


-- ---------- 商品分类表 ----------
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        VARCHAR(50)  NOT NULL COMMENT '分类名，如「教材书籍」',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB COMMENT='商品分类表（一级分类）';


-- ---------- 商品表（乐观锁核心） ----------
CREATE TABLE `product` (
  `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title`       VARCHAR(100)   NOT NULL COMMENT '商品标题',
  `description` VARCHAR(500)   DEFAULT NULL COMMENT '商品描述',
  `image_url`   VARCHAR(255)   DEFAULT NULL COMMENT '商品主图',
  `price`       DECIMAL(10,2)  NOT NULL COMMENT '单价',
  `stock`       INT            NOT NULL DEFAULT 0 COMMENT '库存，扣减后不得为负',
  -- 说明：计划书原文为 tinyint，此处改用 varchar 存枚举名。
  -- 原因：MyBatis 内置 EnumTypeHandler 零配置即可双向映射，
  -- 且直接查库可读性（ON_SALE）远优于数字码（1）。
  `status`      VARCHAR(20)    NOT NULL DEFAULT 'ON_SALE' COMMENT '状态 ON_SALE/OFF_SALE',
  `category_id` BIGINT         NOT NULL COMMENT '分类ID，外键→category.id',
  `seller_id`   BIGINT         NOT NULL COMMENT '卖家ID，外键→user.id',
  `version`     INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，每次更新+1',
  `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_seller`   (`seller_id`),
  KEY `idx_product_status`   (`status`),
  KEY `idx_product_create`   (`create_time`),
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`),
  CONSTRAINT `fk_product_seller`   FOREIGN KEY (`seller_id`)   REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='商品表，version 为乐观锁字段';


-- ---------- 订单主表 ----------
CREATE TABLE `order` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`    VARCHAR(64)   NOT NULL COMMENT '订单号，雪花算法生成',
  `buyer_id`    BIGINT        NOT NULL COMMENT '买家ID，外键→user.id',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status`      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PAID/CANCELLED',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `pay_time`    DATETIME      DEFAULT NULL COMMENT '支付时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_buyer` (`buyer_id`),
  KEY `idx_order_create` (`create_time`),
  CONSTRAINT `fk_order_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='订单主表';


-- ---------- 订单项表 ----------
CREATE TABLE `order_item` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`   BIGINT        NOT NULL COMMENT '订单ID，外键→order.id',
  `product_id` BIGINT        NOT NULL COMMENT '商品ID，外键→product.id',
  `quantity`   INT           NOT NULL COMMENT '购买数量',
  `price`      DECIMAL(10,2) NOT NULL COMMENT '下单时单价快照',
  PRIMARY KEY (`id`),
  KEY `idx_item_order`   (`order_id`),
  KEY `idx_item_product` (`product_id`),
  CONSTRAINT `fk_item_order`   FOREIGN KEY (`order_id`)   REFERENCES `order` (`id`),
  CONSTRAINT `fk_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB COMMENT='订单项表，订单与商品的一对多';


-- ---------- 通知表 ----------
CREATE TABLE `notification` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '接收用户ID，外键→user.id',
  `content`     VARCHAR(500) NOT NULL COMMENT '通知内容',
  `is_read`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读 0否1是',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_notif_user_read` (`user_id`, `is_read`),
  KEY `idx_notif_create` (`create_time`),
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='异步通知表，下单后由线程池写入';


-- ---------- 操作日志表 ----------
CREATE TABLE `operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       DEFAULT NULL COMMENT '操作用户ID，外键→user.id',
  `action`      VARCHAR(50)  NOT NULL COMMENT '操作名称，如「下单」',
  `method`      VARCHAR(200) DEFAULT NULL COMMENT '目标方法签名',
  `detail`      VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `result`      VARCHAR(50)  NOT NULL DEFAULT 'SUCCESS' COMMENT '执行结果 SUCCESS/FAILED',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_oplog_user`   (`user_id`),
  KEY `idx_oplog_create` (`create_time`),
  CONSTRAINT `fk_oplog_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='AOP 操作日志表';


-- ============================================================
-- 测试库不预置种子数据，由各测试类自行构造
-- ============================================================
