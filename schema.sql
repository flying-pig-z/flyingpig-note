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

-- 笔记分组表
CREATE TABLE `note_group` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                              `name` varchar(100) NOT NULL COMMENT '分组名称',
                              `knowledge_base_id` bigint(20) NOT NULL COMMENT '知识库ID',
                              `parent_id` bigint(20) DEFAULT NULL COMMENT '父分组ID',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`),
                              KEY `idx_knowledge_base_id` (`knowledge_base_id`),
                              KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记分组表';

-- 笔记表
CREATE TABLE `note` (
                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                        `title` varchar(200) NOT NULL COMMENT '笔记标题',
                        `content` longtext COMMENT '笔记内容',
                        `knowledge_base_id` bigint(20) NOT NULL COMMENT '知识库ID',
                        `group_id` bigint(20) DEFAULT NULL COMMENT '分组ID',
                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        KEY `idx_knowledge_base_id` (`knowledge_base_id`),
                        KEY `idx_group_id` (`group_id`),
                        KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

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