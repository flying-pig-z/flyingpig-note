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
 * 绗旇鍚戦噺绱㈠紩Mapper
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
     * 鏍规嵁鐭ヨ瘑搴揑D鍒犻櫎鎵€鏈夊悜閲忕储寮?
     */
    @Delete("DELETE FROM note_vector_index WHERE knowledge_base_id = #{knowledgeBaseId}")
    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
