package fun.flyingpig.note.service.note;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.entity.Note;

import java.util.List;

public interface NoteService extends IService<Note> {

    List<Note> getKnowledgeBaseNotes(Long knowledgeBaseId, Long userId);

    List<Note> getKnowledgeBaseNotes(Long knowledgeBaseId);

    List<Note> searchNotes(Long knowledgeBaseId, String keyword, Long userId);

    List<Note> searchNotes(Long knowledgeBaseId, String keyword);

    Note getOwnedNoteById(Long id, Long userId);

    Note createNote(Long userId, NoteDTO dto);

    Note createNote(NoteDTO dto);

    List<Note> batchCreateNotes(List<Note> notes);

    Note updateNote(Long userId, Long id, NoteDTO dto);

    Note updateNote(Long id, NoteDTO dto);

    Note updateNoteGroup(Long userId, Long id, Long groupId);

    Note updateNoteGroup(Long id, Long groupId);

    boolean deleteNoteAndUpdateCount(Long userId, Long id);

    boolean deleteNoteAndUpdateCount(Long id);

    Note createNoteAndUpdateCount(NoteDTO dto);

    List<Note> batchCreateNotesAndUpdateCount(List<Note> notes);
}
