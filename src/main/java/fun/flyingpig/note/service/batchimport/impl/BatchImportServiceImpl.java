package fun.flyingpig.note.service.batchimport.impl;

import fun.flyingpig.note.dto.BatchImportResult;
import fun.flyingpig.note.dto.ImportDetail;
import fun.flyingpig.note.dto.NoteGroupDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteGroup;
import fun.flyingpig.note.service.batchimport.BatchImportService;
import fun.flyingpig.note.service.knowledgebase.KnowledgeBaseService;
import fun.flyingpig.note.service.notegroup.NoteGroupService;
import fun.flyingpig.note.service.note.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private NoteGroupService noteGroupService;

    @Override
    @Transactional
    public BatchImportResult importFolder(MultipartFile[] files, Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在");
        }
        if (!knowledgeBase.getUserId().equals(userId)) {
            throw new SecurityException("无权限操作");
        }

        BatchImportResult result = new BatchImportResult();
        List<ImportDetail> details = new ArrayList<>();

        if (files == null || files.length == 0) {
            result.setImportedCount(0);
            result.setFailedCount(0);
            result.setDetails(details);
            return result;
        }

        int strippedRootDepth = determineSharedRootDepth(files);
        Map<String, Long> groupCache = buildGroupCache(knowledgeBaseId);
        List<Note> notesToInsert = new ArrayList<>();
        List<ImportDetail> pendingDetails = new ArrayList<>();

        for (MultipartFile file : files) {
            String normalizedPath = normalizePath(file.getOriginalFilename());
            ImportDetail detail = new ImportDetail();
            detail.setFileName(normalizedPath);
            details.add(detail);

            try {
                if (!isMarkdownFile(normalizedPath)) {
                    detail.setStatus("SKIPPED");
                    detail.setMessage("非 Markdown 文件，已跳过");
                    continue;
                }

                String content = readFileContent(file);
                Long groupId = resolveGroupId(knowledgeBaseId, normalizedPath, strippedRootDepth, groupCache);
                String importTarget = buildImportTarget(normalizedPath, strippedRootDepth);

                Note note = Note.builder()
                        .title(extractTitleFromFileName(normalizedPath))
                        .content(content)
                        .knowledgeBaseId(knowledgeBaseId)
                        .groupId(groupId)
                        .build();

                notesToInsert.add(note);
                pendingDetails.add(detail);
                detail.setStatus("PENDING");
                detail.setMessage(importTarget == null ? "准备导入到根目录" : "准备导入到 " + importTarget);
            } catch (Exception exception) {
                detail.setStatus("FAILED");
                detail.setMessage("读取文件失败: " + exception.getMessage());
                log.error("批量导入读取文件失败: {}", normalizedPath, exception);
            }
        }

        if (!notesToInsert.isEmpty()) {
            noteService.batchCreateNotes(notesToInsert);
            for (ImportDetail detail : pendingDetails) {
                detail.setStatus("SUCCESS");
                detail.setMessage(detail.getMessage().replace("准备导入", "已导入"));
            }
        }

        result.setImportedCount((int) details.stream().filter(detail -> "SUCCESS".equals(detail.getStatus())).count());
        result.setFailedCount((int) details.stream().filter(detail -> "FAILED".equals(detail.getStatus())).count());
        result.setDetails(details);
        return result;
    }

    private Map<String, Long> buildGroupCache(Long knowledgeBaseId) {
        Map<String, Long> groupCache = new HashMap<>();
        for (NoteGroup group : noteGroupService.getKnowledgeBaseGroups(knowledgeBaseId)) {
            groupCache.put(buildGroupKey(group.getParentId(), group.getName()), group.getId());
        }
        return groupCache;
    }

    private Long resolveGroupId(Long knowledgeBaseId,
                                String filePath,
                                int strippedRootDepth,
                                Map<String, Long> groupCache) {
        List<String> directories = extractDirectorySegments(filePath);
        if (directories.size() <= strippedRootDepth) {
            return null;
        }

        Long parentId = null;
        for (int index = strippedRootDepth; index < directories.size(); index++) {
            String groupName = directories.get(index).trim();
            if (groupName.isEmpty()) {
                continue;
            }

            String cacheKey = buildGroupKey(parentId, groupName);
            Long groupId = groupCache.get(cacheKey);
            if (groupId == null) {
                NoteGroup group = noteGroupService.createGroup(NoteGroupDTO.builder()
                        .name(groupName)
                        .knowledgeBaseId(knowledgeBaseId)
                        .parentId(parentId)
                        .build());
                groupId = group.getId();
                groupCache.put(cacheKey, groupId);
            }
            parentId = groupId;
        }

        return parentId;
    }

    private int determineSharedRootDepth(MultipartFile[] files) {
        String sharedFirstDirectory = null;

        for (MultipartFile file : files) {
            String normalizedPath = normalizePath(file.getOriginalFilename());
            if (!isMarkdownFile(normalizedPath)) {
                continue;
            }

            List<String> directories = extractDirectorySegments(normalizedPath);
            if (directories.isEmpty()) {
                return 0;
            }

            String firstDirectory = directories.get(0);
            if (sharedFirstDirectory == null) {
                sharedFirstDirectory = firstDirectory;
                continue;
            }

            if (!sharedFirstDirectory.equals(firstDirectory)) {
                return 0;
            }
        }

        return sharedFirstDirectory == null ? 0 : 1;
    }

    private String buildImportTarget(String filePath, int strippedRootDepth) {
        List<String> directories = extractDirectorySegments(filePath);
        if (directories.size() <= strippedRootDepth) {
            return null;
        }
        return String.join(" / ", directories.subList(strippedRootDepth, directories.size()));
    }

    private String buildGroupKey(Long parentId, String groupName) {
        return (parentId == null ? "root" : parentId) + "::" + groupName;
    }

    private List<String> extractDirectorySegments(String filePath) {
        String normalizedPath = normalizePath(filePath);
        int lastSeparatorIndex = normalizedPath.lastIndexOf('/');
        if (lastSeparatorIndex < 0) {
            return new ArrayList<>();
        }

        String directoryPath = normalizedPath.substring(0, lastSeparatorIndex);
        if (directoryPath.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> segments = new ArrayList<>();
        for (String segment : directoryPath.split("/")) {
            if (!segment.isBlank()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private boolean isMarkdownFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".md") || lowerFileName.endsWith(".markdown");
    }

    private String extractTitleFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "未命名文档";
        }

        String name = fileName;
        int lastSeparatorIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastSeparatorIndex != -1) {
            name = fileName.substring(lastSeparatorIndex + 1);
        }

        if (name.toLowerCase().endsWith(".md")) {
            return name.substring(0, name.length() - 3);
        }
        if (name.toLowerCase().endsWith(".markdown")) {
            return name.substring(0, name.length() - 9);
        }
        return name;
    }

    private String normalizePath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "未命名文件";
        }
        return fileName.replace('\\', '/').replaceAll("^/+", "");
    }

    private String readFileContent(MultipartFile file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
