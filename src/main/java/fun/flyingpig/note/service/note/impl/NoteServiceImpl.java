package fun.flyingpig.note.service.note.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.exception.BusinessException;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.knowledgebase.KnowledgeBaseService;
import fun.flyingpig.note.service.note.NoteService;
import fun.flyingpig.note.service.notegroup.NoteGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private NoteGroupService noteGroupService;

    @Override
    public List<Note> getKnowledgeBaseNotes(Long knowledgeBaseId) {
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .select(Note::getId, Note::getTitle, Note::getGroupId, Note::getCreateTime, Note::getUpdateTime)
                .orderByAsc(Note::getCreateTime, Note::getId);
        return this.list(queryWrapper);
    }

    @Override
    public List<Note> searchNotes(Long knowledgeBaseId, String keyword) {
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .and(wrapper -> wrapper.like(Note::getTitle, keyword)
                        .or()
                        .like(Note::getContent, keyword))
                .select(Note::getId, Note::getTitle, Note::getGroupId, Note::getCreateTime, Note::getUpdateTime)
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
            validateKnowledgeBaseMatch(note.getKnowledgeBaseId(), dto.getKnowledgeBaseId());
            validateGroupBelongsToKnowledgeBase(dto.getKnowledgeBaseId(), dto.getGroupId());
            note.setTitle(dto.getTitle());
            note.setContent(dto.getContent());
            note.setGroupId(dto.getGroupId());
            this.updateById(note);
        }
        return note;
    }

    @Override
    public Note updateNoteGroup(Long id, Long groupId) {
        Note note = this.getById(id);
        if (note != null) {
            validateGroupBelongsToKnowledgeBase(note.getKnowledgeBaseId(), groupId);
            this.baseMapper.updateGroupIdById(id, groupId);
            note.setGroupId(groupId);
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
        validateGroupBelongsToKnowledgeBase(dto.getKnowledgeBaseId(), dto.getGroupId());

        Note note = Note.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .knowledgeBaseId(dto.getKnowledgeBaseId())
                .groupId(dto.getGroupId())
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

    private void validateKnowledgeBaseMatch(Long expectedKnowledgeBaseId, Long actualKnowledgeBaseId) {
        if (!expectedKnowledgeBaseId.equals(actualKnowledgeBaseId)) {
            throw new BusinessException(400, "文档不能切换到其他知识库");
        }
    }

    private void validateGroupBelongsToKnowledgeBase(Long knowledgeBaseId, Long groupId) {
        if (groupId == null) {
            return;
        }

        NoteGroup group = noteGroupService.getById(groupId);
        if (group == null || !knowledgeBaseId.equals(group.getKnowledgeBaseId())) {
            throw new BusinessException(400, "目标分组不存在或不属于当前知识库");
        }
    }
}
