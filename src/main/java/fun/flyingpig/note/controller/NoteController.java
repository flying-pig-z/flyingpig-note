package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.NoteDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired
    private NoteService noteService;

    /**
     * 获取知识库的笔记列表
     */
    @GetMapping
    public Result<List<Note>> getKnowledgeBaseNotes(@RequestParam Long knowledgeBaseId) {
        List<Note> notes = noteService.getKnowledgeBaseNotes(knowledgeBaseId);
        return Result.success(notes);
    }

    /**
     * 搜索笔记
     */
    @GetMapping("/search")
    public Result<List<Note>> searchNotes(
            @RequestParam Long knowledgeBaseId,
            @RequestParam String keyword) {
        List<Note> notes = noteService.searchNotes(knowledgeBaseId, keyword);
        return Result.success(notes);
    }

    /**
     * 创建笔记
     */
    @PostMapping
    public Result<Note> createNote(@Valid @RequestBody NoteDTO dto) {
        Note note = noteService.createNote(dto);
        return Result.success("创建成功", note);
    }

    /**
     * 获取笔记详情
     */
    @GetMapping("/{id}")
    public Result<Note> getNoteById(@PathVariable Long id) {
        Note note = noteService.getById(id);
        if (note == null) {
            return Result.error("笔记不存在");
        }

        return Result.success(note);
    }

    /**
     * 更新笔记
     */
    @PutMapping("/{id}")
    public Result<Note> updateNote(@PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        Note note = noteService.updateNote(id, dto);
        if (note == null) {
            return Result.error("笔记不存在");
        }

        return Result.success("更新成功", note);
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/{id}")
    public Result deleteNote(@PathVariable Long id) {
        boolean success = noteService.deleteNoteAndUpdateCount(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("笔记不存在");
        }
    }
}