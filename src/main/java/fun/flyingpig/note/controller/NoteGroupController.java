package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.dto.NoteGroupMoveDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.service.notegroup.NoteGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/note-groups")
public class NoteGroupController {

    @Autowired
    private NoteGroupService noteGroupService;

    @GetMapping
    public Result<List<NoteGroup>> getKnowledgeBaseGroups(@RequestParam Long knowledgeBaseId) {
        List<NoteGroup> groups = noteGroupService.getKnowledgeBaseGroups(knowledgeBaseId);
        return Result.success(groups);
    }

    @PostMapping
    public Result<NoteGroup> createGroup(@Valid @RequestBody NoteGroupDTO dto) {
        NoteGroup group = noteGroupService.createGroup(dto);
        return Result.success("创建成功", group);
    }

    @PutMapping("/{id}")
    public Result<NoteGroup> updateGroup(@PathVariable Long id, @Valid @RequestBody NoteGroupDTO dto) {
        NoteGroup group = noteGroupService.updateGroup(id, dto);
        if (group == null) {
            return Result.error("分组不存在");
        }
        return Result.success("更新成功", group);
    }

    @PutMapping("/{id}/move")
    public Result<NoteGroup> moveGroup(@PathVariable Long id, @RequestBody NoteGroupMoveDTO dto) {
        NoteGroup group = noteGroupService.moveGroup(id, dto.getParentId());
        if (group == null) {
            return Result.error("分组不存在");
        }
        return Result.success("更新成功", group);
    }

    @DeleteMapping("/{id}")
    public Result deleteGroup(@PathVariable Long id) {
        boolean success = noteGroupService.deleteGroupCascade(id);
        if (success) {
            return Result.success("删除成功");
        }
        return Result.error("分组不存在");
    }
}
