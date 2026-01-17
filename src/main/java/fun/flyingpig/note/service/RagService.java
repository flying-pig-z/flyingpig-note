package fun.flyingpig.note.service;

import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;

/**
 * RAG服务接口
 */
public interface RagService {

    /**
     * 基于知识库回答问题
     *
     * @param queryDTO 查询请求
     * @return 回答结果
     */
    RagAnswerDTO answer(RagQueryDTO queryDTO);

    /**
     * 更新知识库索引
     *
     * @param updateIndexDTO 更新索引请求
     * @return 更新结果
     */
    UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO);
    
    /**
     * 强制更新知识库索引（删除所有现有索引后重建）
     *
     * @param updateIndexDTO 更新索引请求
     * @return 更新结果
     */
    UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO);
}