package fun.flyingpig.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.entity.NoteVectorIndex;
import fun.flyingpig.note.mapper.NoteVectorIndexMapper;
import fun.flyingpig.note.service.INoteVectorIndexService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记向量索引服务实现类
 */
@Service
public class NoteVectorIndexServiceImpl extends ServiceImpl<NoteVectorIndexMapper, NoteVectorIndex> implements INoteVectorIndexService {

    @Override
    public List<NoteVectorIndex> selectByKnowledgeBaseIds(List<Long> knowledgeBaseIds) {
        LambdaQueryWrapper<NoteVectorIndex> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(NoteVectorIndex::getKnowledgeBaseId, knowledgeBaseIds);
        return this.list(queryWrapper);
    }

    @Override
    public LocalDateTime getLatestUpdateTimeByNoteId(Long noteId) {
        return this.baseMapper.getLatestUpdateTimeByNoteId(noteId);
    }

    @Override
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        this.baseMapper.deleteByKnowledgeBaseId(knowledgeBaseId);
    }
}