package fun.flyingpig.note.service;

import fun.flyingpig.note.dto.QdrantSearchResult;
import fun.flyingpig.note.entity.QdrantPoint;

import java.util.List;


/**
 * Qdrant向量数据库服务接口
 */
public interface QdrantService {

    /**
     * 初始化Collection（如果不存在则创建）
     */
    void initCollection();

    /**
     * 插入或更新向量点
     *
     * @param pointId        点ID（使用NoteVectorIndex的ID）
     * @param vector         向量数据
     * @param noteId         笔记ID
     * @param knowledgeBaseId 知识库ID
     * @param chunkIndex     分块索引
     * @param chunkContent   分块内容
     */
    void upsertPoint(Long pointId, List<Double> vector, Long noteId, Long knowledgeBaseId,
                     Integer chunkIndex, String chunkContent);

    /**
     * 批量插入向量点
     *
     * @param points 点列表
     */
    void upsertPoints(List<QdrantPoint> points);

    /**
     * 根据笔记ID删除所有相关向量
     *
     * @param noteId 笔记ID
     */
    void deleteByNoteId(Long noteId);

    /**
     * 根据知识库ID删除所有相关向量
     *
     * @param knowledgeBaseId 知识库ID
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 向量搜索
     *
     * @param queryVector      查询向量
     * @param knowledgeBaseIds 知识库ID列表（过滤条件）
     * @param topK             返回结果数量
     * @return 搜索结果列表
     */
    List<QdrantSearchResult> search(List<Double> queryVector, List<Long> knowledgeBaseIds, long topK);

}