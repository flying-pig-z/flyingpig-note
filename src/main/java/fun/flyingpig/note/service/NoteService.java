package fun.flyingpig.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.entity.Note;

import java.util.List;

public interface NoteService extends IService<Note> {

    /**
     * 获取知识库的笔记列表
     */
     List<Note> getKnowledgeBaseNotes(Long knowledgeBaseId);

    /**
     * 搜索笔记
     */
    List<Note> searchNotes(Long knowledgeBaseId, String keyword);

    /**
     * 创建笔记
     */
    Note createNote(NoteDTO dto);

    /**
     * 批量创建笔记
     */
    List<Note> batchCreateNotes(List<Note> notes);

    /**
     * 更新笔记
     */
    Note updateNote(Long id, NoteDTO dto);

    /**
     * 删除笔记
     */
    boolean deleteNoteAndUpdateCount(Long id);

    /**
     * 原子性地创建笔记并更新计数
     */
    Note createNoteAndUpdateCount(NoteDTO dto);
    
    /**
     * 原子性地批量创建笔记并更新计数
     */
    List<Note> batchCreateNotesAndUpdateCount(List<Note> notes);
}