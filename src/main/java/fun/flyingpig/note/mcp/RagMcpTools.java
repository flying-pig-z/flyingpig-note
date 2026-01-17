package fun.flyingpig.note.mcp;

import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.service.KnowledgeBaseService;
import fun.flyingpig.note.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG MCP 工具类 (使用 Spring AI MCP 专用注解)
 *
 * 使用 @McpTool 和 @McpToolParam 注解自动暴露为 MCP 工具
 * Spring AI 会自动扫描并注册这些工具到 MCP Server
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagMcpTools {

    private final RagService ragService;
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * RAG 问答查询
     */
    @McpTool(name = "rag_query",
            description = "基于知识库进行RAG问答查询。根据用户问题，从指定知识库中检索相关内容并生成回答。")
    public RagAnswerDTO ragQuery(
            @McpToolParam(description = "用户的问题", required = true)
            String question,

            @McpToolParam(description = "要查询的知识库ID列表", required = true)
            List<Long> knowledgeBaseIds,

            @McpToolParam(description = "返回的最相关文档数量，默认为5", required = false)
            Integer topK) {

        log.info("MCP Tool调用: rag_query, 问题: {}, 知识库IDs: {}", question, knowledgeBaseIds);

        RagQueryDTO queryDTO = new RagQueryDTO();
        queryDTO.setQuestion(question);
        queryDTO.setKnowledgeBaseIds(knowledgeBaseIds);
        queryDTO.setTopK(topK);

        return ragService.answer(queryDTO);
    }

    /**
     * 获取用户的知识库列表
     */
    @McpTool(name = "list_knowledge_bases",
            description = "获取指定用户的所有知识库列表。")
    public List<KnowledgeBase> listKnowledgeBases(
            @McpToolParam(description = "用户ID", required = true)
            Long userId) {

        log.info("MCP Tool调用: list_knowledge_bases, 用户ID: {}", userId);
        return knowledgeBaseService.getUserKnowledgeBases(userId);
    }

    /**
     * 搜索知识库
     */
    @McpTool(name = "search_knowledge_bases",
            description = "根据关键词搜索用户的知识库。")
    public List<KnowledgeBase> searchKnowledgeBases(
            @McpToolParam(description = "用户ID", required = true)
            Long userId,

            @McpToolParam(description = "搜索关键词", required = true)
            String keyword) {

        log.info("MCP Tool调用: search_knowledge_bases, 用户ID: {}, 关键词: {}", userId, keyword);
        return knowledgeBaseService.searchKnowledgeBases(userId, keyword);
    }
}