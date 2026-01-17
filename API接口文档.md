# Note System API 接口文档

## 项目概述
Note System 是一个基于RAG技术的笔记管理系统，支持知识库管理和智能问答功能。

## 服务基础信息
- 服务地址：http://localhost:9091
- 协议：HTTP
- 请求格式：JSON
- 响应格式：JSON
- 认证方式：JWT Token

## 全局响应格式
所有接口统一返回以下格式：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

- `code`: 响应状态码
  - 200: 成功
  - 500: 失败
  - 其他自定义状态码
- `message`: 响应消息
- `data`: 响应数据

## 1. 认证接口 (Auth)

### 1.1 用户登录
- **接口路径**: POST `/api/auth/login`
- **功能描述**: 用户登录验证
- **请求参数**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 1,
        "username": "admin",
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
      }
    }
  }
  ```
- **错误响应**:
  ```json
  {
    "code": 500,
    "message": "用户名或密码错误",
    "data": null
  }
  ```

### 1.2 用户注册
- **接口路径**: POST `/api/auth/register`
- **功能描述**: 用户注册
- **请求参数**:
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 2,
        "username": "newuser",
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
      }
    }
  }
  ```

### 1.3 用户登出
- **接口路径**: POST `/api/auth/logout`
- **功能描述**: 用户登出（JWT无状态，客户端删除token即可）
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "登出成功",
    "data": null
  }
  ```

### 1.4 获取当前用户信息
- **接口路径**: GET `/api/auth/current`
- **功能描述**: 获取当前登录用户信息
- **认证要求**: 需要在Header中携带Authorization: Bearer {token}
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "username": "admin",
      "createTime": "2024-01-01 12:00:00",
      "updateTime": "2024-01-01 12:00:00"
    }
  }
  ```
- **错误响应**:
  ```json
  {
    "code": 401,
    "message": "未登录",
    "data": null
  }
  ```

## 2. 知识库接口 (Knowledge Base)

### 2.1 获取用户知识库列表
- **接口路径**: GET `/api/knowledge-bases`
- **功能描述**: 获取当前用户的所有知识库
- **认证要求**: 需要在Header中携带Authorization: Bearer {token}
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "title": "个人知识库",
        "description": "我的个人笔记集合",
        "userId": 1,
        "noteCount": 5,
        "indexUpdateTime": "2024-01-01 12:00:00",
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
      }
    ]
  }
  ```

### 2.2 搜索知识库
- **接口路径**: GET `/api/knowledge-bases/search?keyword={keyword}`
- **功能描述**: 根据关键词搜索当前用户的知识库
- **认证要求**: 需要在Header中携带Authorization: Bearer {token}
- **请求参数**:
  - `keyword`: 搜索关键词
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "title": "个人知识库",
        "description": "我的个人笔记集合",
        "userId": 1,
        "noteCount": 5,
        "indexUpdateTime": "2024-01-01 12:00:00",
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
      }
    ]
  }
  ```

### 2.3 创建知识库
- **接口路径**: POST `/api/knowledge-bases`
- **功能描述**: 创建新的知识库
- **认证要求**: 需要在Header中携带Authorization: Bearer {token}
- **请求参数**:
  ```json
  {
    "title": "知识库标题",
    "description": "知识库描述"
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "创建成功",
    "data": {
      "id": 2,
      "title": "新知识库",
      "description": "知识库描述",
      "userId": 1,
      "noteCount": 0,
      "indexUpdateTime": null,
      "createTime": "2024-01-01 12:00:00",
      "updateTime": "2024-01-01 12:00:00"
    }
  }
  ```

### 2.4 更新知识库
- **接口路径**: PUT `/api/knowledge-bases/{id}`
- **功能描述**: 更新知识库信息
- **认证要求**: 需要在Header中携带Authorization: Bearer {token}
- **路径参数**:
  - `id`: 知识库ID
- **请求参数**:
  ```json
  {
    "title": "更新后的标题",
    "description": "更新后的描述"
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "更新成功",
    "data": {
      "id": 2,
      "title": "更新后的标题",
      "description": "更新后的描述",
      "userId": 1,
      "noteCount": 0,
      "indexUpdateTime": null,
      "createTime": "2024-01-01 12:00:00",
      "updateTime": "2024-01-01 12:00:00"
    }
  }
  ```

### 2.5 删除知识库
- **接口路径**: DELETE `/api/knowledge-bases/{id}`
- **功能描述**: 删除指定知识库
- **认证要求**: 需要在Header中携带Authorization: Bearer {token}
- **路径参数**:
  - `id`: 知识库ID
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "删除成功",
    "data": null
  }
  ```

## 3. 笔记接口 (Note)

### 3.1 获取知识库的笔记列表
- **接口路径**: GET `/api/notes?knowledgeBaseId={knowledgeBaseId}`
- **功能描述**: 获取指定知识库下的所有笔记
- **请求参数**:
  - `knowledgeBaseId`: 知识库ID
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "title": "笔记标题",
        "content": "笔记内容",
        "knowledgeBaseId": 1,
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
      }
    ]
  }
  ```

### 3.2 搜索笔记
- **接口路径**: GET `/api/notes/search?knowledgeBaseId={knowledgeBaseId}&keyword={keyword}`
- **功能描述**: 在指定知识库中搜索笔记
- **请求参数**:
  - `knowledgeBaseId`: 知识库ID
  - `keyword`: 搜索关键词
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": [
      {
        "id": 1,
        "title": "匹配的笔记",
        "content": "笔记内容",
        "knowledgeBaseId": 1,
        "createTime": "2024-01-01 12:00:00",
        "updateTime": "2024-01-01 12:00:00"
      }
    ]
  }
  ```

### 3.3 创建笔记
- **接口路径**: POST `/api/notes`
- **功能描述**: 创建新的笔记
- **请求参数**:
  ```json
  {
    "title": "笔记标题",
    "content": "笔记内容",
    "knowledgeBaseId": 1
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "创建成功",
    "data": {
      "id": 1,
      "title": "笔记标题",
      "content": "笔记内容",
      "knowledgeBaseId": 1,
      "createTime": "2024-01-01 12:00:00",
      "updateTime": "2024-01-01 12:00:00"
    }
  }
  ```

### 3.4 获取笔记详情
- **接口路径**: GET `/api/notes/{id}`
- **功能描述**: 获取指定笔记的详细信息
- **路径参数**:
  - `id`: 笔记ID
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "id": 1,
      "title": "笔记标题",
      "content": "笔记内容",
      "knowledgeBaseId": 1,
      "createTime": "2024-01-01 12:00:00",
      "updateTime": "2024-01-01 12:00:00"
    }
  }
  ```
- **错误响应**:
  ```json
  {
    "code": 500,
    "message": "笔记不存在",
    "data": null
  }
  ```

### 3.5 更新笔记
- **接口路径**: PUT `/api/notes/{id}`
- **功能描述**: 更新指定笔记
- **路径参数**:
  - `id`: 笔记ID
- **请求参数**:
  ```json
  {
    "title": "更新后的标题",
    "content": "更新后的内容",
    "knowledgeBaseId": 1
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "更新成功",
    "data": {
      "id": 1,
      "title": "更新后的标题",
      "content": "更新后的内容",
      "knowledgeBaseId": 1,
      "createTime": "2024-01-01 12:00:00",
      "updateTime": "2024-01-01 12:00:00"
    }
  }
  ```
- **错误响应**:
  ```json
  {
    "code": 500,
    "message": "笔记不存在",
    "data": null
  }
  ```

### 3.6 删除笔记
- **接口路径**: DELETE `/api/notes/{id}`
- **功能描述**: 删除指定笔记
- **路径参数**:
  - `id`: 笔记ID
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "删除成功",
    "data": null
  }
  ```
- **错误响应**:
  ```json
  {
    "code": 500,
    "message": "笔记不存在",
    "data": null
  }
  ```

## 4. RAG接口 (Retrieval-Augmented Generation)

### 4.1 RAG问答接口
- **接口路径**: POST `/api/rag/answer`
- **功能描述**: 基于指定知识库进行向量检索和回答
- **请求参数**:
  ```json
  {
    "question": "用户问题",
    "knowledgeBaseIds": [1, 2],
    "topK": 5
  }
  ```
  - `question`: 用户提出的问题
  - `knowledgeBaseIds`: 指定的知识库ID列表
  - `topK`: 返回的相关文档数量，默认为5
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "answer": "AI生成的回答内容",
      "relevantDocuments": [
        {
          "noteId": 1,
          "noteTitle": "相关笔记标题",
          "content": "相关内容片段",
          "score": 0.85
        }
      ]
    }
  }
  ```

### 4.2 更新知识库索引
- **接口路径**: POST `/api/rag/updateIndex`
- **功能描述**: 为指定知识库的笔记生成向量索引
- **请求参数**:
  ```json
  {
    "knowledgeBaseId": 1
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "knowledgeBaseId": 1,
      "insertedCount": 2,
      "updatedCount": 1,
      "skippedCount": 3,
      "deletedCount": 0,
      "details": [
        {
          "noteId": 1,
          "noteTitle": "笔记标题",
          "action": "INSERT",
          "message": "索引已创建"
        }
      ]
    }
  }
  ```

### 4.3 强制更新知识库索引
- **接口路径**: POST `/api/rag/forceUpdateIndex`
- **功能描述**: 删除指定知识库的所有现有索引并重新创建，用于处理维度变化等情况
- **请求参数**:
  ```json
  {
    "knowledgeBaseId": 1
  }
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "操作成功",
    "data": {
      "knowledgeBaseId": 1,
      "insertedCount": 5,
      "updatedCount": 0,
      "skippedCount": 0,
      "deletedCount": 0,
      "details": [
        {
          "noteId": 1,
          "noteTitle": "笔记标题",
          "action": "INSERT",
          "message": "索引已重建"
        }
      ]
    }
  }
  ```

## 5. 批量导入接口 (Batch Import)

### 5.1 批量导入Markdown文件
- **接口路径**: POST `/api/batch-import/folder`
- **功能描述**: 批量导入Markdown文件到指定知识库
- **请求参数**:
  - `files`: 上传的多个Markdown文件
  - `knowledgeBaseId`: 目标知识库ID
- **请求示例** (multipart/form-data):
  ```
  Content-Type: multipart/form-data
  files: [file1.md, file2.md, ...]
  knowledgeBaseId: 1
  ```
- **响应示例**:
  ```json
  {
    "code": 200,
    "message": "批量导入完成",
    "data": {
      "totalFiles": 5,
      "successCount": 4,
      "failedCount": 1,
      "details": [
        {
          "filename": "file1.md",
          "status": "success",
          "message": "导入成功"
        },
        {
          "filename": "file2.md",
          "status": "error",
          "message": "导入失败原因"
        }
      ]
    }
  }
  ```

## 错误码说明
- `200`: 成功
- `401`: 未授权访问
- `500`: 服务器内部错误
- 其他自定义错误码根据具体业务场景定义

## 认证说明
大部分接口需要用户认证，请求时需要在Header中携带JWT Token:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

## 注意事项
1. 所有时间字段格式为 `yyyy-MM-dd HH:mm:ss`
2. 所有ID字段为Long类型
3. 请求参数中带有`@NotBlank`或`@NotNull`注解的字段为必填项
4. 对于涉及文件上传的接口，请使用multipart/form-data格式