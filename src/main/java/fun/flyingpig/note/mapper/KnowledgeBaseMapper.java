package fun.flyingpig.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 更新知识库的笔记数量
     */
    @Update("UPDATE knowledge_base SET note_count = note_count + #{delta} WHERE id = #{kbId}")
    void updateNoteCount(@Param("kbId") Long kbId, @Param("delta") Integer delta);
    
    /**
     * 重新计算知识库的笔记数量
     */
    @Update("UPDATE knowledge_base kb SET note_count = (SELECT COUNT(*) FROM note n WHERE n.knowledge_base_id = kb.id) WHERE kb.id = #{kbId}")
    void updateNoteCountByKnowledgeBaseId(@Param("kbId") Long kbId);
}