package fun.flyingpig.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.KnowledgeBaseService;
import fun.flyingpig.note.service.NoteService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public List<Note> getKnowledgeBaseNotes(Long knowledgeBaseId) {
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .select(Note::getId, Note::getTitle) // 内容不传回进一步优化性能，但是需要修改前端
                .orderByDesc(Note::getUpdateTime);
        return this.list(queryWrapper);
    }

    @Override
    public List<Note> searchNotes(Long knowledgeBaseId, String keyword) {
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .and(wrapper -> wrapper.like(Note::getTitle, keyword)
                        .or()
                        .like(Note::getContent, keyword))
                .select(Note::getId, Note::getTitle) // 内容不传回进一步优化性能，但是需要修改前端
                .orderByDesc(Note::getUpdateTime);
        return this.list(queryWrapper);
    }

    @Override
    public Note createNote(NoteDTO dto) {
        // 调用统一方法以保持一致性
        return createNoteAndUpdateCount(dto);
    }

    @Override
    public List<Note> batchCreateNotes(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用MyBatis-Plus的批量保存功能
        this.saveBatch(notes);

        // 批量更新知识库笔记数量（只需要更新一次）
        if (!notes.isEmpty()) {
            Long knowledgeBaseId = notes.get(0).getKnowledgeBaseId();
            long count = notes.size();
            knowledgeBaseService.updateNoteCount(knowledgeBaseId, (int) count);
        }

        return notes;
    }

    @Override
    public Note updateNote(Long id, NoteDTO dto) {
        Note note = this.getById(id);
        if (note != null) {
            note.setTitle(dto.getTitle());
            note.setContent(dto.getContent());
            this.updateById(note);
        }
        return note;
    }

    @Override
    public boolean deleteNoteAndUpdateCount(Long id) {
        Note note = this.getById(id);
        if (note != null) {
            boolean result = this.removeById(id);
            if (result) {
                // 更新知识库笔记数量
                knowledgeBaseService.updateNoteCount(note.getKnowledgeBaseId(), -1);
            }
            return result;
        }
        return false;
    }
    
    @Override
    public Note createNoteAndUpdateCount(NoteDTO dto) {
        Note note = Note.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .knowledgeBaseId(dto.getKnowledgeBaseId())
                .build();
        this.save(note);

        // 原子性地更新知识库笔记数量
        knowledgeBaseService.updateNoteCount(dto.getKnowledgeBaseId(), 1);

        return note;
    }
    
    @Override
    public List<Note> batchCreateNotesAndUpdateCount(List<Note> notes) {
        if (notes == null || notes.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用MyBatis-Plus的批量保存功能
        this.saveBatch(notes);

        // 原子性地更新知识库笔记数量（只需要更新一次）
        if (!notes.isEmpty()) {
            Long knowledgeBaseId = notes.get(0).getKnowledgeBaseId();
            long count = notes.size();
            knowledgeBaseService.updateNoteCount(knowledgeBaseId, (int) count);
        }

        return notes;
    }
}