package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.dto.NoteGroupMoveDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.service.notegroup.NoteGroupService;
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
@RequestMapping("/api/note-groups")
@RequiredArgsConstructor
public class NoteGroupController {

    private final NoteGroupService noteGroupService;
    private final NoteSecurityService noteSecurityService;

    @GetMapping
    public Result<List<NoteGroup>> getKnowledgeBaseGroups(@RequestParam Long knowledgeBaseId) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success(noteGroupService.getKnowledgeBaseGroups(knowledgeBaseId, userId));
    }

    @PostMapping
    public Result<NoteGroup> createGroup(@Valid @RequestBody NoteGroupDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("创建成功", noteGroupService.createGroup(userId, dto));
    }

    @PutMapping("/{id}")
    public Result<NoteGroup> updateGroup(@PathVariable Long id, @Valid @RequestBody NoteGroupDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("更新成功", noteGroupService.updateGroup(userId, id, dto));
    }

    @PutMapping("/{id}/move")
    public Result<NoteGroup> moveGroup(@PathVariable Long id, @RequestBody NoteGroupMoveDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("更新成功", noteGroupService.moveGroup(userId, id, dto.getParentId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteGroupService.deleteGroupCascade(userId, id);
        return Result.success("删除成功", null);
    }
}
