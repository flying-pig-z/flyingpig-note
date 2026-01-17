package fun.flyingpig.note.service;

import fun.flyingpig.note.dto.BatchImportResult;
import fun.flyingpig.note.dto.ImportDetail;
import fun.flyingpig.note.entity.Note;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BatchImportService {

    /**
     * 批量导入Markdown文件到知识库
     * @param files 要导入的文件数组
     * @param knowledgeBaseId 目标知识库ID
     * @param userId 当前用户ID
     * @return 导入结果
     */
    BatchImportResult importFolder(MultipartFile[] files, Long knowledgeBaseId, Long userId);
}