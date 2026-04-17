package fun.flyingpig.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.entity.Note;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    
    /**
     * 根据知识库ID删除所有笔记
     */
    @Delete("DELETE FROM note WHERE knowledge_base_id = #{knowledgeBaseId}")
    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    @Update("UPDATE note SET group_id = #{groupId} WHERE group_id = #{oldGroupId}")
    int updateGroupIdByGroupId(@Param("oldGroupId") Long oldGroupId, @Param("groupId") Long groupId);

    @Update("UPDATE note SET group_id = #{groupId} WHERE id = #{id}")
    int updateGroupIdById(@Param("id") Long id, @Param("groupId") Long groupId);
}
