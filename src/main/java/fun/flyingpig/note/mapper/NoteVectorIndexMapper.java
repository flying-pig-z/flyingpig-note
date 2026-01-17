package fun.flyingpig.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.entity.NoteVectorIndex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;

/**
 * 笔记向量索引Mapper
 */
@Mapper
public interface NoteVectorIndexMapper extends BaseMapper<NoteVectorIndex> {
    /**
     * 根据笔记ID获取最新的索引更新时间
     */
    @Select("SELECT MAX(update_time) FROM note_vector_index WHERE note_id = #{noteId}")
    LocalDateTime getLatestUpdateTimeByNoteId(@Param("noteId") Long noteId);

    /**
     * 根据知识库ID删除所有向量索引
     */
    @Delete("DELETE FROM note_vector_index WHERE knowledge_base_id = #{knowledgeBaseId}")
    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}