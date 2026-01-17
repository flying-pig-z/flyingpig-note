package fun.flyingpig.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.entity.Note;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    
    /**
     * 根据知识库ID删除所有笔记
     */
    @Delete("DELETE FROM note WHERE knowledge_base_id = #{knowledgeBaseId}")
    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);
}
