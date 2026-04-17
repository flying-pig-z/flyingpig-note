package fun.flyingpig.note.service.vectorindex;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.entity.NoteVectorIndex;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 笔记向量索引服务接口
 */
public interface INoteVectorIndexService extends IService<NoteVectorIndex> {

    /**
     * 按知识库查询每篇笔记最近一次索引更新时间
     */
    Map<Long, LocalDateTime> getLatestUpdateTimeMapByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 根据知识库 ID 删除该知识库下的全部向量索引
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
