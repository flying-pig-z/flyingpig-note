-- 创建数据库
CREATE DATABASE IF NOT EXISTS note_system DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE note_system;

-- 用户表
CREATE TABLE `user` (
                     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                     `username` varchar(50) NOT NULL COMMENT '用户名',
                     `password` varchar(255) NOT NULL COMMENT '密码',
                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                     PRIMARY KEY (`id`),
                     UNIQUE KEY `uk_username` (`username`),
                     KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 知识库表
CREATE TABLE `knowledge_base` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `title` varchar(100) NOT NULL COMMENT '知识库标题',
                                  `description` text COMMENT '知识库描述',
                                  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
                                  `note_count` int(11) NOT NULL DEFAULT 0 COMMENT '笔记数量',
                                  `index_update_time` datetime DEFAULT NULL COMMENT '索引最后更新时间',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_user_id` (`user_id`),
                                  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 笔记表
CREATE TABLE `note` (
                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                        `title` varchar(200) NOT NULL COMMENT '笔记标题',
                        `content` longtext COMMENT '笔记内容',
                        `knowledge_base_id` bigint(20) NOT NULL COMMENT '知识库ID',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        KEY `idx_knowledge_base_id` (`knowledge_base_id`),
                        KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- 插入测试数据
INSERT INTO `user` (`username`, `password`, `email`) VALUES
                                                         ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVeIni', 'admin@example.com'),
                                                         ('test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVeIni', 'test@example.com');

INSERT INTO `knowledge_base` (`title`, `description`, `user_id`, `note_count`) VALUES
                                                                                   ('技术笔记', '记录编程技术、框架学习等内容', 1, 4),
                                                                                   ('读书笔记', '记录阅读心得和书籍摘要', 1, 1),
                                                                                   ('工作日志', '记录工作中的问题和解决方案', 1, 1);

INSERT INTO `note` (`title`, `content`, `knowledge_base_id`) VALUES
                                                                 ('JavaScript基础', '# JavaScript基础\n\n## 变量声明\n\n在JavaScript中，有三种声明变量的方式：\n\n- `var`：函数作用域\n- `let`：块级作用域\n- `const`：常量，块级作用域\n\n```javascript\nlet name = ''Hello'';\nconst age = 25;\n```', 1),
                                                                 ('ES6特性', '# ES6新特性\n\n## 箭头函数\n\n```javascript\nconst add = (a, b) => a + b;\n```\n\n## 模板字符串\n\n```javascript\nconst message = `Hello, ${name}!`;\n```', 1),
                                                                 ('Promise与异步', '# Promise与异步编程\n\n## Promise基础\n\nPromise是异步编程的一种解决方案。\n\n```javascript\nconst promise = new Promise((resolve, reject) => {\n    // 异步操作\n});\n```', 1),
                                                                 ('React框架', '# React框架学习\n\n## 组件基础\n\nReact是基于组件的前端框架。\n\n```jsx\nfunction Welcome(props) {\n    return <h1>Hello, {props.name}</h1>;\n}\n```', 1),
                                                                 ('《JavaScript高级程序设计》', '# JavaScript高级程序设计\n\n这本书是JavaScript学习的经典教材。\n\n## 主要内容\n\n- 基础语法\n- 面向对象编程\n- DOM操作\n- 事件处理', 2),
                                                                 ('项目部署问题', '# 项目部署遇到的问题\n\n## 问题描述\n\n今天部署项目时遇到了端口占用的问题。\n\n## 解决方案\n\n1. 查看端口占用：`netstat -ano | findstr :8080`\n2. 结束进程：`taskkill /PID <PID> /F`', 3);

-- 笔记向量索引表
 CREATE TABLE `note_vector_index` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                  `note_id` bigint(20) NOT NULL COMMENT '笔记ID',
                                  `knowledge_base_id` bigint(20) NOT NULL COMMENT '知识库ID',
                                  `chunk_index` int(11) NOT NULL DEFAULT 0 COMMENT '分块索引',
                                  `chunk_content` text NOT NULL COMMENT '分块内容',
                                  `embedding` text NOT NULL COMMENT '向量嵌入(JSON数组)',
                                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '索引创建时间',
                                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '索引更新时间',
                                  PRIMARY KEY (`id`),
                                  KEY `idx_note_id` (`note_id`),
                                  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
                                  UNIQUE KEY `uk_note_chunk` (`note_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记向量索引表';