package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.BatchImportResult;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.service.BatchImportService;
import fun.flyingpig.note.util.jwt.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/batch-import")
public class BatchImportController {

    @Autowired
    private BatchImportService batchImportService;

    /**
     * 批量导入Markdown文件到知识库
     */
    @PostMapping("/folder")
    public Result<BatchImportResult> importFolder(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId) {
        Long userId = UserContext.getCurrentUserId();
        BatchImportResult result = batchImportService.importFolder(files, knowledgeBaseId, userId);
        return Result.success("批量导入完成", result);
    }
}