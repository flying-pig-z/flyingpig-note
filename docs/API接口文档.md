# API 接口文档

本文档描述 `note-system` 的 HTTP 接口。`README.md` 负责项目说明，这里只放接口约定、请求参数和调用示例。

## 一、基本信息

- 默认服务地址：`http://localhost:9092`
- 接口前缀：`/api`
- 数据格式：`application/json`
- 字符编码：`UTF-8`

## 二、认证说明

除以下接口外，其余 `/api/**` 默认都需要携带 JWT：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`

请求头格式：

```http
Authorization: Bearer <JWT>
```

## 三、统一返回格式

### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 失败响应

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null
}
```

常见状态语义：

- `200`：成功
- `400`：参数错误或业务校验失败
- `401`：未登录、Token 缺失或已过期
- `403`：无权访问当前资源
- `404`：资源不存在
- `500`：系统异常

## 四、数据对象说明

### 4.1 User

```json
{
  "id": 1,
  "username": "demo",
  "createTime": "2026-04-18 12:00:00",
  "updateTime": "2026-04-18 12:00:00"
}
```

### 4.2 KnowledgeBase

```json
{
  "id": 1,
  "title": "学习笔记",
  "description": "用于记录学习内容",
  "userId": 1,
  "noteCount": 10,
  "indexUpdateTime": "2026-04-18 12:00:00",
  "createTime": "2026-04-18 12:00:00",
  "updateTime": "2026-04-18 12:00:00"
}
```

### 4.3 Note

列表接口返回的是摘要对象，通常不包含 `content`；详情接口会返回完整内容。

```json
{
  "id": 1,
  "title": "第一篇笔记",
  "content": "# Markdown 内容",
  "knowledgeBaseId": 1,
  "groupId": 2,
  "createTime": "2026-04-18 12:00:00",
  "updateTime": "2026-04-18 12:00:00"
}
```

### 4.4 NoteGroup

```json
{
  "id": 2,
  "name": "后端",
  "knowledgeBaseId": 1,
  "parentId": null,
  "createTime": "2026-04-18 12:00:00",
  "updateTime": "2026-04-18 12:00:00"
}
```

## 五、认证接口

### 5.1 注册

- 方法：`POST`
- 路径：`/api/auth/register`

请求体：

```json
{
  "username": "demo",
  "password": "123456"
}
```

字段说明：

- `username`：必填
- `password`：必填

响应 `data`：

```json
{
  "token": "xxx",
  "tokenType": "Bearer",
  "userInfo": {
    "id": 1,
    "username": "demo",
    "createTime": "2026-04-18 12:00:00",
    "updateTime": "2026-04-18 12:00:00"
  }
}
```

### 5.2 登录

- 方法：`POST`
- 路径：`/api/auth/login`

请求体：

```json
{
  "username": "demo",
  "password": "123456"
}
```

响应结构同注册接口。

### 5.3 登出

- 方法：`POST`
- 路径：`/api/auth/logout`

说明：

- 当前实现主要由前端清理本地 Token
- 接口返回成功消息，不做服务端会话存储

### 5.4 获取当前用户

- 方法：`GET`
- 路径：`/api/auth/current`
- 需要认证：是

## 六、知识库接口

### 6.1 查询当前用户知识库

- 方法：`GET`
- 路径：`/api/knowledge-bases`

### 6.2 搜索知识库

- 方法：`GET`
- 路径：`/api/knowledge-bases/search`

查询参数：

- `keyword`：必填，搜索关键词

### 6.3 创建知识库

- 方法：`POST`
- 路径：`/api/knowledge-bases`

请求体：

```json
{
  "title": "学习笔记",
  "description": "用于记录学习内容"
}
```

字段说明：

- `title`：必填，最长 100
- `description`：可选，最长 500

### 6.4 更新知识库

- 方法：`PUT`
- 路径：`/api/knowledge-bases/{id}`

请求体与创建接口一致。

### 6.5 删除知识库

- 方法：`DELETE`
- 路径：`/api/knowledge-bases/{id}`

说明：

- 会级联删除该知识库下的笔记、分组、向量索引和 Qdrant 数据

## 七、笔记接口

### 7.1 查询知识库下笔记

- 方法：`GET`
- 路径：`/api/notes`

查询参数：

- `knowledgeBaseId`：必填

### 7.2 搜索笔记

- 方法：`GET`
- 路径：`/api/notes/search`

查询参数：

- `knowledgeBaseId`：必填
- `keyword`：必填

### 7.3 创建笔记

- 方法：`POST`
- 路径：`/api/notes`

请求体：

```json
{
  "title": "Spring Boot 笔记",
  "content": "# 标题\n\n正文内容",
  "knowledgeBaseId": 1,
  "groupId": 2
}
```

字段说明：

- `title`：必填，最长 200
- `content`：可选
- `knowledgeBaseId`：必填
- `groupId`：可选，为空表示不归属分组

### 7.4 获取笔记详情

- 方法：`GET`
- 路径：`/api/notes/{id}`

### 7.5 更新笔记

- 方法：`PUT`
- 路径：`/api/notes/{id}`

请求体与创建笔记一致。

限制：

- 笔记更新时不能切换到其他知识库

### 7.6 调整笔记分组

- 方法：`PUT`
- 路径：`/api/notes/{id}/group`

请求体：

```json
{
  "groupId": 2
}
```

说明：

- `groupId` 可为 `null`
- 目标分组必须属于当前笔记所在知识库

### 7.7 删除笔记

- 方法：`DELETE`
- 路径：`/api/notes/{id}`

## 八、分组接口

### 8.1 查询知识库分组

- 方法：`GET`
- 路径：`/api/note-groups`

查询参数：

- `knowledgeBaseId`：必填

### 8.2 创建分组

- 方法：`POST`
- 路径：`/api/note-groups`

请求体：

```json
{
  "name": "后端",
  "knowledgeBaseId": 1,
  "parentId": null
}
```

字段说明：

- `name`：必填，最长 100
- `knowledgeBaseId`：必填
- `parentId`：可选

### 8.3 更新分组

- 方法：`PUT`
- 路径：`/api/note-groups/{id}`

请求体与创建分组一致。

限制：

- 分组更新时不能切换到其他知识库

### 8.4 移动分组

- 方法：`PUT`
- 路径：`/api/note-groups/{id}/move`

请求体：

```json
{
  "parentId": 3
}
```

说明：

- `parentId` 可为 `null`
- 不允许把分组移动到自己的子分组下

### 8.5 删除分组

- 方法：`DELETE`
- 路径：`/api/note-groups/{id}`

说明：

- 会递归删除子分组
- 会同步删除分组内笔记及其索引数据

## 九、批量导入接口

### 9.1 导入 Markdown 文件夹

- 方法：`POST`
- 路径：`/api/batch-import/folder`
- Content-Type：`multipart/form-data`

表单字段：

- `knowledgeBaseId`：知识库 ID
- `files`：多个文件，支持 `.md` / `.markdown`

响应 `data`：

```json
{
  "importedCount": 3,
  "failedCount": 1,
  "details": [
    {
      "fileName": "Java/集合/List.md",
      "status": "SUCCESS",
      "message": "已导入到 Java / 集合"
    }
  ]
}
```

说明：

- 导入时会自动把目录映射为分组
- 非 Markdown 文件会跳过

## 十、RAG 接口

### 10.1 普通问答

- 方法：`POST`
- 路径：`/api/rag/answer`

请求体：

```json
{
  "question": "什么是机器学习？",
  "knowledgeBaseIds": [1],
  "history": [
    {
      "role": "user",
      "content": "先介绍一下监督学习"
    },
    {
      "role": "assistant",
      "content": "监督学习是..."
    }
  ]
}
```

字段说明：

- `question`：必填
- `knowledgeBaseIds`：必填，至少一个
- `history`：可选，最多 10 条
- `history.role`：仅支持 `user`、`assistant`

响应 `data`：

```json
{
  "answer": "机器学习是...",
  "relevantDocuments": [
    {
      "noteId": 1,
      "noteTitle": "机器学习入门",
      "content": "机器学习是一种...",
      "score": 0.92
    }
  ]
}
```

### 10.2 流式问答

- 方法：`POST`
- 路径：`/api/rag/answer/stream`
- 响应类型：`text/event-stream`

请求体与普通问答一致。

SSE 事件：

- `delta`：增量文本
- `done`：最终结果
- `error`：错误信息

`delta` 示例：

```text
event: delta
data: {"content":"机器学习是"}
```

`done` 示例：

```text
event: done
data: {"answer":"机器学习是...","relevantDocuments":[...]}
```

### 10.3 增量更新索引

- 方法：`POST`
- 路径：`/api/rag/updateIndex`

请求体：

```json
{
  "knowledgeBaseId": 1
}
```

响应 `data`：

```json
{
  "knowledgeBaseId": 1,
  "insertedCount": 2,
  "updatedCount": 3,
  "skippedCount": 5,
  "deletedCount": 1,
  "details": [
    {
      "noteId": 10,
      "noteTitle": "Redis 笔记",
      "action": "UPDATE",
      "message": "索引已更新"
    }
  ]
}
```

### 10.4 流式增量更新索引

- 方法：`POST`
- 路径：`/api/rag/updateIndex/stream`
- 响应类型：`text/event-stream`

SSE 事件：

- `progress`：进度更新
- `done`：最终结果
- `error`：错误信息

### 10.5 强制重建索引

- 方法：`POST`
- 路径：`/api/rag/forceUpdateIndex`

请求体与增量更新一致。

说明：

- 会先清空知识库现有索引
- 再为知识库下所有笔记重建索引

### 10.6 流式强制重建索引

- 方法：`POST`
- 路径：`/api/rag/forceUpdateIndex/stream`
- 响应类型：`text/event-stream`

SSE 事件与流式增量更新一致。

`progress` 示例：

```text
event: progress
data: {"stage":"PROCESSING","processedNotes":3,"totalNotes":10,"progressPercent":30}
```
