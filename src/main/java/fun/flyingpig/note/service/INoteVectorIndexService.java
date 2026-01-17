package fun.flyingpig.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.entity.NoteVectorIndex;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记向量索引服务接口
 */
public interface INoteVectorIndexService extends IService<NoteVectorIndex> {

    /**
     * 根据知识库ID列表查询所有向量索引
     */
    List<NoteVectorIndex> selectByKnowledgeBaseIds(List<Long> knowledgeBaseIds);

    /**
     * 根据笔记ID获取最新的索引更新时间
     */
    LocalDateTime getLatestUpdateTimeByNoteId(Long noteId);

    /**
     * 根据知识库ID删除所有向量索引
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}