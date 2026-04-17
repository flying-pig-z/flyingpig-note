package fun.flyingpig.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.entity.NoteGroup;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NoteGroupMapper extends BaseMapper<NoteGroup> {

    @Delete("DELETE FROM note_group WHERE knowledge_base_id = #{knowledgeBaseId}")
    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") Long knowledgeBaseId);

    @Update("UPDATE note_group SET parent_id = #{parentId} WHERE id = #{id}")
    int updateParentId(@Param("id") Long id, @Param("parentId") Long parentId);

    @Update("UPDATE note_group SET parent_id = #{parentId} WHERE parent_id = #{oldParentId}")
    int updateParentIdByParentId(@Param("oldParentId") Long oldParentId, @Param("parentId") Long parentId);
}
