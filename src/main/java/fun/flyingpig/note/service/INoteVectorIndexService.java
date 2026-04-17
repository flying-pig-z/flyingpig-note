package fun.flyingpig.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.entity.NoteVectorIndex;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 绗旇鍚戦噺绱㈠紩鏈嶅姟鎺ュ彛
 */
public interface INoteVectorIndexService extends IService<NoteVectorIndex> {

    /**
     * 按知识库查询每篇笔记最近一次索引更新时间。
     */
    Map<Long, LocalDateTime> getLatestUpdateTimeMapByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 鏍规嵁鐭ヨ瘑搴揑D鍒犻櫎鎵€鏈夊悜閲忕储寮?
     */
    void deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
