package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.dto.NoteGroupAssignmentDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.service.note.NoteService;
import fun.flyingpig.note.service.security.NoteSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteSecurityService noteSecurityService;

    @GetMapping
    public Result<List<Note>> getKnowledgeBaseNotes(@RequestParam Long knowledgeBaseId) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success(noteService.getKnowledgeBaseNotes(knowledgeBaseId, userId));
    }

    @GetMapping("/search")
    public Result<List<Note>> searchNotes(@RequestParam Long knowledgeBaseId, @RequestParam String keyword) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success(noteService.searchNotes(knowledgeBaseId, keyword, userId));
    }

    @PostMapping
    public Result<Note> createNote(@Valid @RequestBody NoteDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("创建成功", noteService.createNote(userId, dto));
    }

    @GetMapping("/{id}")
    public Result<Note> getNoteById(@PathVariable Long id) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success(noteService.getOwnedNoteById(id, userId));
    }

    @PutMapping("/{id}")
    public Result<Note> updateNote(@PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("更新成功", noteService.updateNote(userId, id, dto));
    }

    @PutMapping("/{id}/group")
    public Result<Note> updateNoteGroup(@PathVariable Long id, @RequestBody NoteGroupAssignmentDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("更新成功", noteService.updateNoteGroup(userId, id, dto.getGroupId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteService.deleteNoteAndUpdateCount(userId, id);
        return Result.success("删除成功", null);
    }
}
