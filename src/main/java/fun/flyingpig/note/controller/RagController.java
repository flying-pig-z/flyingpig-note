package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.*;
import fun.flyingpig.note.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * RAG控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * RAG问答接口
     * 基于指定知识库进行向量检索和回答
     *
     * @param queryDTO 查询请求，包含问题和知识库ID列表
     * @return 回答结果，包含AI回答和相关文档
     */
    @PostMapping("/answer")
    public Result<RagAnswerDTO> answer(@RequestBody @Validated RagQueryDTO queryDTO) {
        log.info("收到RAG问答请求: {}", queryDTO.getQuestion());
        RagAnswerDTO answer = ragService.answer(queryDTO);
        return Result.success(answer);
    }

    /**
     * 更新知识库索引接口
     * 使用API为指定知识库的笔记生成向量索引
     *
     * @param updateIndexDTO 更新索引请求，包含知识库ID
     * @return 更新结果，包含新增、更新、跳过的笔记数量
     */
    @PostMapping("/updateIndex")
    public Result<UpdateIndexResultDTO> updateIndex(@RequestBody @Validated UpdateIndexDTO updateIndexDTO) {
        log.info("收到更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        UpdateIndexResultDTO result = ragService.updateIndex(updateIndexDTO);
        return Result.success(result);
    }
    
    /**
     * 强制更新知识库索引接口
     * 删除指定知识库的所有现有索引并重新创建，用于处理维度变化等情况
     *
     * @param updateIndexDTO 更新索引请求，包含知识库ID
     * @return 更新结果，包含重建的笔记数量
     */
    @PostMapping("/forceUpdateIndex")
    public Result<UpdateIndexResultDTO> forceUpdateIndex(@RequestBody @Validated UpdateIndexDTO updateIndexDTO) {
        log.info("收到强制更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        UpdateIndexResultDTO result = ragService.forceUpdateIndex(updateIndexDTO);
        return Result.success(result);
    }
}