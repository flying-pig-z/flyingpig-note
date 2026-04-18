# MCP 使用文档

## 1. 摘要

`note-system` 提供基于 `Streamable HTTP` 的 MCP 服务，默认地址为 `http://127.0.0.1:9092/mcp`，鉴权方式为 `Authorization: Bearer <JWT>`。JWT 可通过登录或注册接口获取，MCP 工具只允许访问当前用户自己的数据。

## 2. 接口链接

- HTTP API 文档：[API接口文档.md](API接口文档.md)
- 项目说明：[README.md](../README.md)

## 3. 功能

- 知识库：支持列表、详情、搜索、创建、更新、删除。
- 笔记：支持列表、详情、搜索、创建、更新、移动分组、删除。
- 分组：支持列表、创建、重命名、移动、删除。
