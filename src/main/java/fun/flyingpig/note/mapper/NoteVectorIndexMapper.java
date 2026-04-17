package fun.flyingpig.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.dto.NoteIndexLatestUpdateDTO;
import fun.flyingpig.note.entity.NoteVectorIndex;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 笔记向量索引 Mapper
 */
@Mapper
public interface NoteVectorIndexMapper extends BaseMapper<NoteVectorIndex> {

    /**
     * 按知识库查询每篇笔记最近一次索引更新时间，只返回必要字段。
     */
    @Select("""
            SELECT note_id AS noteId, MAX(update_time) AS latestUpdateTime
            FROM note_vector_index
            WHERE knowledge_base_id = #{knowledgeBaseId}
            GROUP BY note_id
            """)
    List<NoteIndexLatestUpdateDTO> selectLatestUpdateTimesByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    /**
     * 根据知识库 ID 删除所有向量索引
     */
    @Delete("DELETE FROM note_vector_index WHERE knowledge_base_id = #{knowledgeBaseId}")
    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
