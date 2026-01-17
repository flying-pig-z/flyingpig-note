package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.KnowledgeBaseDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.service.KnowledgeBaseService;
import fun.flyingpig.note.util.jwt.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 获取当前用户的知识库列表
     */
    @GetMapping
    public Result<List<KnowledgeBase>> getUserKnowledgeBases() {
        Long userId = UserContext.getCurrentUserId();
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getUserKnowledgeBases(userId);
        return Result.success(knowledgeBases);
    }

    /**
     * 搜索知识库
     */
    @GetMapping("/search")
    public Result<List<KnowledgeBase>> searchKnowledgeBases(@RequestParam String keyword) {
        Long userId = UserContext.getCurrentUserId();
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.searchKnowledgeBases(userId, keyword);
        return Result.success(knowledgeBases);
    }

    /**
     * 创建知识库
     */
    @PostMapping
    public Result<KnowledgeBase> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(userId, dto);
        return Result.success("创建成功", knowledgeBase);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public Result<KnowledgeBase> updateKnowledgeBase(
            @PathVariable Long id, @Valid @RequestBody KnowledgeBaseDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseService.updateKnowledgeBase(id, userId, dto);
        return Result.success("更新成功", knowledgeBase);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result deleteKnowledgeBase(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        knowledgeBaseService.deleteKnowledgeBaseWithPermissionCheck(id, userId);
        return Result.success("删除成功");
    }
}
