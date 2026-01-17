# 飞猪笔记

## 项目介绍

飞猪笔记是一个基于 RAG（检索增强生成）和 MCP（Model Context Protocol）技术的智能笔记应用，支持本地和在线部署。系统采用前后端分离架构，结合人工智能技术，为用户提供高效、智能的笔记管理和知识检索体验。

### 项目功能

* **用户认证管理**: 用户注册、登录
* **知识库管理**: 支持多知识库分类管理，便于组织和归档笔记内容
* **笔记管理与编写**: 支持Markdown格式的笔记创建、编辑、删除等基本操作
* **批量导入功能**: 支持批量导入Markdown文件，快速构建知识库
* **智能AI问答**: 基于指定知识库的RAG问答系统，提供精准的智能回答
* **开放MCP接口**: 提供标准的MCP接口，支持与其他AI工具集成

### 技术栈

* **后端框架**: Spring Boot 3.1.5 + Java 17
* **持久化层**: MyBatis Plus 3.5.7 + MySQL 8.0.33
* **向量数据库**: Qdrant 1.12.0（用于存储和检索文本向量）
* **AI集成**: 智谱清言API（Embedding-3模型）、自定义问答模型
* **安全认证**: JWT（JSON Web Token）
* **前端技术**: HTML5、CSS3、JavaScript、AJAX
* **其他工具**: Hutool工具库、FastJSON、OkHttp

## 快速开始

### 环境要求

* Java 17 或更高版本
* Maven 3.6.0 或更高版本
* MySQL 8.0 或更高版本
* Qdrant 向量数据库

### 项目部署

#### 1. 数据库配置

1. 创建MySQL数据库：`note_system`
2. 执行 `schema.sql` 脚本创建表结构
3. 修改 `application.yml` 中的数据库连接信息

#### 2. 向量数据库配置

1. 部署Qdrant向量数据库服务
2. 在 `application.yml` 中配置Qdrant连接参数

#### 3. AI服务配置

1. 配置智谱清言API密钥（用于生成文本向量）
2. 配置自定义问答模型API密钥

#### 4. 启动应用

1. 克隆项目代码
2. 修改 `application.yml` 中的相关配置
3. 使用 Maven 构建项目：`mvn clean install`
4. 启动应用：`java -jar note-system-1.0.0.jar`

## 关键流程

### 1. RAG（检索增强生成）

#### 【1】文档预处理

* 先按 Markdown 格式语义分割文档，并且限制每段长度，超过长度的大块切割，小块合并
* 然后将每一个块生成向量存放到向量数据库 Qdrant 中

#### 【2】AI问答
* 将问题也转化为向量
* 从向量数据库中检索与问题最相似的文档块
* 将检索到的文档块与问题一起输入给AI模型，生成答案

### 2. MCP（Model Context Protocol）

开放MCP接口，使用 Claude Desktop 作为客户端进行测试，支持外部AI工具通过MCP协议访问知识库问答功能。

## API接口

系统提供了RESTful API接口，主要包括：

* `/api/auth/*` - 认证相关接口
* `/api/knowledge-base/*` - 知识库管理接口
* `/api/note/*` - 笔记管理接口
* `/api/rag/*` - RAG问答接口
* `/api/batch-import/*` - 批量导入接口
* `/mcp/*` - MCP服务接口

## 配置说明

主要配置项位于 `application.yml` 文件中：

* `server.port` - 应用启动端口，默认9091
* `spring.datasource.*` - 数据库连接配置
* `qdrant.*` - Qdrant向量数据库配置
* `zhipu.*` - 智谱清言API配置
* `rag.*` - RAG系统参数配置
* `jwt.*` - JWT认证配置

## 注意事项

1. 部署前请确保已正确配置数据库和向量数据库
2. 请妥善保管API密钥等敏感信息
3. 生产环境部署时注意修改默认密码和密钥
4. 定期备份数据库和向量数据

