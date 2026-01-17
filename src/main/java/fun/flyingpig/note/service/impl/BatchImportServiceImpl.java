package fun.flyingpig.note.service.impl;

import fun.flyingpig.note.dto.BatchImportResult;
import fun.flyingpig.note.dto.ImportDetail;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.service.BatchImportService;
import fun.flyingpig.note.service.KnowledgeBaseService;
import fun.flyingpig.note.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BatchImportServiceImpl implements BatchImportService {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private NoteService noteService;

    @Override
    public BatchImportResult importFolder(MultipartFile[] files, Long knowledgeBaseId, Long userId) {
        // 验证知识库是否存在且属于当前用户
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new SecurityException("无权限操作");
        }

        BatchImportResult result = new BatchImportResult();
        List<ImportDetail> details = new ArrayList<>();

        List<Note> notesToInsert = new ArrayList<>();
        Map<String, ImportDetail> fileNameToDetailMap = new HashMap<>();

        // 首先处理所有文件，准备要插入的笔记数据
        for (MultipartFile file : files) {
            ImportDetail detail = new ImportDetail();
            detail.setFileName(file.getOriginalFilename());

            try {
                // 只处理Markdown文件
                if (isMarkdownFile(file.getOriginalFilename())) {
                    // 读取文件内容
                    String content = readFileContent(file);

                    // 提取标题（使用文件名作为标题，去掉.md后缀）
                    String title = extractTitleFromFileName(file.getOriginalFilename());

                    // 创建笔记对象
                    Note note = Note.builder()
                            .title(title)
                            .content(content)
                            .knowledgeBaseId(knowledgeBaseId)
                            .build();

                    notesToInsert.add(note);
                    detail.setStatus("PENDING"); // 标记为待插入
                    detail.setMessage("准备插入");

                } else {
                    detail.setStatus("SKIPPED");
                    detail.setMessage("非Markdown文件，已跳过");
                }
            } catch (Exception e) {
                detail.setStatus("FAILED");
                detail.setMessage("读取文件失败: " + e.getMessage());
                log.error("读取文件失败: {}", file.getOriginalFilename(), e);
            }

            fileNameToDetailMap.put(file.getOriginalFilename(), detail);
            details.add(detail);
        }

        // 批量插入笔记
        List<Note> insertedNotes = noteService.batchCreateNotes(notesToInsert);

        // 更新成功插入的笔记的详细信息
        int importedCount = 0;
        int failedCount = 0;

        // 创建一个映射，用于快速查找已插入的笔记
        Map<String, Note> titleToNoteMap = new HashMap<>();
        for (Note note : insertedNotes) {
            titleToNoteMap.put(note.getTitle(), note);
        }

        // 更新details列表中成功插入的笔记信息
        for (ImportDetail detail : details) {
            if ("PENDING".equals(detail.getStatus())) {
                // 查找对应的已插入笔记
                Note insertedNote = titleToNoteMap.get(extractTitleFromFileName(detail.getFileName()));
                if (insertedNote != null) {
                    detail.setStatus("SUCCESS");
                    detail.setMessage("导入成功");
                    importedCount++;
                } else {
                    detail.setStatus("FAILED");
                    detail.setMessage("插入后未找到对应笔记");
                    failedCount++;
                }
            } else if ("SKIPPED".equals(detail.getStatus())) {
                failedCount++; // 跳过的文件也计入失败计数
            } else if ("FAILED".equals(detail.getStatus())) {
                failedCount++;
            }
        }

        result.setImportedCount(importedCount);
        result.setFailedCount(failedCount);
        result.setDetails(details);

        return result;
    }

    /**
     * 检查是否为Markdown文件
     */
    private boolean isMarkdownFile(String fileName) {
        if (fileName == null) return false;
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".md") || lowerFileName.endsWith(".markdown");
    }

    /**
     * 从文件名提取标题
     */
    private String extractTitleFromFileName(String fileName) {
        if (fileName == null) return "未命名笔记";

        // 获取文件名（不包含路径）
        String name = fileName;
        int lastSeparatorIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastSeparatorIndex != -1) {
            name = fileName.substring(lastSeparatorIndex + 1);
        }

        // 去掉.md或.markdown后缀
        if (name.toLowerCase().endsWith(".md")) {
            return name.substring(0, name.length() - 3);
        } else if (name.toLowerCase().endsWith(".markdown")) {
            return name.substring(0, name.length() - 9);
        }

        return name;
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(MultipartFile file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}