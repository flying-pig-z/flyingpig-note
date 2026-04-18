package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.BatchImportResult;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.service.batchimport.BatchImportService;
import fun.flyingpig.note.service.security.NoteSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/batch-import")
@RequiredArgsConstructor
public class BatchImportController {

    private final BatchImportService batchImportService;
    private final NoteSecurityService noteSecurityService;

    @PostMapping("/folder")
    public Result<BatchImportResult> importFolder(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId
    ) {
        Long userId = noteSecurityService.requireCurrentUserId();
        BatchImportResult result = batchImportService.importFolder(files, knowledgeBaseId, userId);
        return Result.success("批量导入完成", result);
    }
}
