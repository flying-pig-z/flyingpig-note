package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.KnowledgeBaseDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.service.knowledgebase.KnowledgeBaseService;
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
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final NoteSecurityService noteSecurityService;

    @GetMapping
    public Result<List<KnowledgeBase>> getUserKnowledgeBases() {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success(knowledgeBaseService.getUserKnowledgeBases(userId));
    }

    @GetMapping("/search")
    public Result<List<KnowledgeBase>> searchKnowledgeBases(@RequestParam String keyword) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success(knowledgeBaseService.searchKnowledgeBases(userId, keyword));
    }

    @PostMapping
    public Result<KnowledgeBase> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("创建成功", knowledgeBaseService.createKnowledgeBase(userId, dto));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeBase> updateKnowledgeBase(@PathVariable Long id, @Valid @RequestBody KnowledgeBaseDTO dto) {
        Long userId = noteSecurityService.requireCurrentUserId();
        return Result.success("更新成功", knowledgeBaseService.updateKnowledgeBase(id, userId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        Long userId = noteSecurityService.requireCurrentUserId();
        knowledgeBaseService.deleteKnowledgeBaseWithPermissionCheck(id, userId);
        return Result.success("删除成功", null);
    }
}
