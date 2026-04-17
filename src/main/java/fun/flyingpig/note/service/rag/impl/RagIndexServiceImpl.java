package fun.flyingpig.note.service.rag.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.flyingpig.note.dto.IndexProgressDTO;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteVectorIndex;
import fun.flyingpig.note.mapper.KnowledgeBaseMapper;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.qdrant.QdrantClient;
import fun.flyingpig.note.service.rag.NoteIndexService;
import fun.flyingpig.note.service.rag.RagIndexService;
import fun.flyingpig.note.service.vectorindex.INoteVectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexServiceImpl implements RagIndexService {

    private static final Consumer<IndexProgressDTO> NO_OP_PROGRESS_CONSUMER = progress -> { };

    private final NoteMapper noteMapper;
    private final INoteVectorIndexService noteVectorIndexService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final NoteIndexService noteIndexService;
    private final QdrantClient qdrantClient;

    @Transactional
    @Override
    public UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO) {
        return updateIndex(updateIndexDTO, NO_OP_PROGRESS_CONSUMER);
    }

    @Transactional
    @Override
    public UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer) {
        Consumer<IndexProgressDTO> safeProgressConsumer = progressConsumer != null ? progressConsumer : NO_OP_PROGRESS_CONSUMER;
        Long kbId = updateIndexDTO.getKnowledgeBaseId();
        log.info("开始更新知识库索引，知识库ID: {}", kbId);

        emitProgress(safeProgressConsumer, createProgress(
                kbId, false, "PREPARING", "正在加载知识库笔记和索引状态",
                0, 0, 0, 0, 0, 0, null, null, null
        ));

        List<Note> notes = listNotesByKnowledgeBaseId(kbId);
        Map<Long, LocalDateTime> latestIndexTimeMap =
                noteVectorIndexService.getLatestUpdateTimeMapByKnowledgeBaseId(kbId);
        int totalNotes = notes.size();

        Set<Long> indexedNoteIds = new HashSet<>(latestIndexTimeMap.keySet());
        Set<Long> actualNoteIds = notes.stream()
                .map(Note::getId)
                .collect(Collectors.toSet());

        Set<Long> noteIdsToDelete = new HashSet<>(indexedNoteIds);
        noteIdsToDelete.removeAll(actualNoteIds);

        log.info(
                "知识库 {} 索引准备完成，笔记总数: {}, 已有索引笔记: {}, 待清理索引: {}",
                kbId,
                totalNotes,
                indexedNoteIds.size(),
                noteIdsToDelete.size()
        );

        if (!noteIdsToDelete.isEmpty()) {
            emitProgress(safeProgressConsumer, createProgress(
                    kbId, false, "CLEANING", "正在清理已删除笔记的旧索引",
                    totalNotes, 0, 0, 0, 0, noteIdsToDelete.size(), null, null, null
            ));
        }
        cleanupDeletedNoteIndexes(noteIdsToDelete);

        UpdateIndexResultDTO result = new UpdateIndexResultDTO();
        result.setKnowledgeBaseId(kbId);
        result.setInsertedCount(0);
        result.setUpdatedCount(0);
        result.setSkippedCount(0);
        result.setDeletedCount(noteIdsToDelete.size());

        List<UpdateIndexResultDTO.NoteIndexDetail> details = new ArrayList<>();
        int processedNotes = 0;
        for (Note note : notes) {
            UpdateIndexResultDTO.NoteIndexDetail detail = new UpdateIndexResultDTO.NoteIndexDetail();
            detail.setNoteId(note.getId());
            detail.setNoteTitle(note.getTitle());

            LocalDateTime latestIndexTime = latestIndexTimeMap.get(note.getId());
            String currentAction;
            if (latestIndexTime == null) {
                noteIndexService.writeIndexForNote(note);
                detail.setAction("INSERT");
                detail.setMessage("新建索引成功");
                result.setInsertedCount(result.getInsertedCount() + 1);
                currentAction = "INSERT";
            } else if (latestIndexTime.isBefore(note.getUpdateTime())) {
                removeNoteIndexes(note.getId());
                noteIndexService.writeIndexForNote(note);
                detail.setAction("UPDATE");
                detail.setMessage("索引已更新");
                result.setUpdatedCount(result.getUpdatedCount() + 1);
                currentAction = "UPDATE";
            } else {
                detail.setAction("SKIP");
                detail.setMessage("索引已是最新");
                result.setSkippedCount(result.getSkippedCount() + 1);
                currentAction = "SKIP";
            }

            details.add(detail);
            processedNotes++;

            emitProgress(safeProgressConsumer, createProgress(
                    kbId, false, "PROCESSING", buildProgressMessage(detail, processedNotes, totalNotes),
                    totalNotes, processedNotes, result.getInsertedCount(), result.getUpdatedCount(),
                    result.getSkippedCount(), result.getDeletedCount(), note.getId(), note.getTitle(), currentAction
            ));
            logNoteProgress(kbId, processedNotes, totalNotes, note, currentAction, result);
        }

        result.setDetails(details);
        updateKnowledgeBaseIndexTime(kbId);

        emitProgress(safeProgressConsumer, createProgress(
                kbId, false, "COMPLETED", "索引更新完成",
                totalNotes, totalNotes, result.getInsertedCount(), result.getUpdatedCount(),
                result.getSkippedCount(), result.getDeletedCount(), null, null, null
        ));

        log.info("知识库索引更新完成，新增: {}, 更新: {}, 跳过: {}, 清理: {}",
                result.getInsertedCount(), result.getUpdatedCount(), result.getSkippedCount(), result.getDeletedCount());
        return result;
    }

    @Transactional
    @Override
    public UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO) {
        return forceUpdateIndex(updateIndexDTO, NO_OP_PROGRESS_CONSUMER);
    }

    @Transactional
    @Override
    public UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO, Consumer<IndexProgressDTO> progressConsumer) {
        Consumer<IndexProgressDTO> safeProgressConsumer = progressConsumer != null ? progressConsumer : NO_OP_PROGRESS_CONSUMER;
        Long kbId = updateIndexDTO.getKnowledgeBaseId();
        log.info("开始强制更新知识库索引，知识库ID: {}", kbId);

        emitProgress(safeProgressConsumer, createProgress(
                kbId, true, "PREPARING", "正在加载知识库笔记",
                0, 0, 0, 0, 0, 0, null, null, null
        ));

        List<Note> notes = listNotesByKnowledgeBaseId(kbId);
        int totalNotes = notes.size();

        log.info("知识库 {} 强制更新准备完成，笔记总数: {}", kbId, totalNotes);

        emitProgress(safeProgressConsumer, createProgress(
                kbId, true, "CLEANING", "正在删除现有索引数据",
                totalNotes, 0, 0, 0, 0, 0, null, null, null
        ));

        noteVectorIndexService.deleteByKnowledgeBaseId(kbId);
        qdrantClient.deleteByKnowledgeBaseId(kbId);
        log.info("已删除知识库 {} 的所有现有索引(MySQL + Qdrant)", kbId);

        UpdateIndexResultDTO result = new UpdateIndexResultDTO();
        result.setKnowledgeBaseId(kbId);
        result.setInsertedCount(0);
        result.setUpdatedCount(0);
        result.setSkippedCount(0);
        result.setDeletedCount(0);

        List<UpdateIndexResultDTO.NoteIndexDetail> details = new ArrayList<>();
        int processedNotes = 0;
        for (Note note : notes) {
            UpdateIndexResultDTO.NoteIndexDetail detail = new UpdateIndexResultDTO.NoteIndexDetail();
            detail.setNoteId(note.getId());
            detail.setNoteTitle(note.getTitle());

            noteIndexService.writeIndexForNote(note);

            detail.setAction("INSERT");
            detail.setMessage("强制重建索引成功");
            details.add(detail);

            result.setInsertedCount(result.getInsertedCount() + 1);
            processedNotes++;

            emitProgress(safeProgressConsumer, createProgress(
                    kbId, true, "PROCESSING", buildProgressMessage(detail, processedNotes, totalNotes),
                    totalNotes, processedNotes, result.getInsertedCount(), result.getUpdatedCount(),
                    result.getSkippedCount(), result.getDeletedCount(), note.getId(), note.getTitle(), "INSERT"
            ));
            logNoteProgress(kbId, processedNotes, totalNotes, note, "INSERT", result);
        }

        result.setDetails(details);
        updateKnowledgeBaseIndexTime(kbId);

        emitProgress(safeProgressConsumer, createProgress(
                kbId, true, "COMPLETED", "强制更新完成",
                totalNotes, totalNotes, result.getInsertedCount(), result.getUpdatedCount(),
                result.getSkippedCount(), result.getDeletedCount(), null, null, null
        ));

        log.info("知识库索引强制更新完成，重建了 {} 个笔记的索引", notes.size());
        return result;
    }

    private IndexProgressDTO createProgress(
            Long knowledgeBaseId,
            boolean forceUpdate,
            String stage,
            String message,
            int totalNotes,
            int processedNotes,
            int insertedCount,
            int updatedCount,
            int skippedCount,
            int deletedCount,
            Long currentNoteId,
            String currentNoteTitle,
            String currentAction
    ) {
        IndexProgressDTO progress = new IndexProgressDTO();
        progress.setKnowledgeBaseId(knowledgeBaseId);
        progress.setForceUpdate(forceUpdate);
        progress.setStage(stage);
        progress.setMessage(message);
        progress.setTotalNotes(totalNotes);
        progress.setProcessedNotes(processedNotes);
        progress.setInsertedCount(insertedCount);
        progress.setUpdatedCount(updatedCount);
        progress.setSkippedCount(skippedCount);
        progress.setDeletedCount(deletedCount);
        progress.setProgressPercent(calculateProgressPercent(totalNotes, processedNotes, stage));
        progress.setCurrentNoteId(currentNoteId);
        progress.setCurrentNoteTitle(currentNoteTitle);
        progress.setCurrentAction(currentAction);
        return progress;
    }

    private int calculateProgressPercent(int totalNotes, int processedNotes, String stage) {
        if ("COMPLETED".equals(stage)) {
            return 100;
        }
        if (totalNotes <= 0) {
            return "CLEANING".equals(stage) ? 20 : 0;
        }
        return Math.min(99, (int) Math.round(processedNotes * 100.0 / totalNotes));
    }

    private void emitProgress(Consumer<IndexProgressDTO> progressConsumer, IndexProgressDTO progress) {
        progressConsumer.accept(progress);
    }

    private String buildProgressMessage(UpdateIndexResultDTO.NoteIndexDetail detail, int processedNotes, int totalNotes) {
        String noteTitle = detail.getNoteTitle() != null && !detail.getNoteTitle().isBlank()
                ? detail.getNoteTitle()
                : "未命名笔记";
        return String.format("正在处理第 %d/%d 篇：%s", processedNotes, totalNotes, noteTitle);
    }

    private void logNoteProgress(
            Long knowledgeBaseId,
            int processedNotes,
            int totalNotes,
            Note note,
            String action,
            UpdateIndexResultDTO result
    ) {
        log.info(
                "知识库 {} 索引进度 {}/{} [{}] 笔记ID: {}, 标题: {}, 新增: {}, 更新: {}, 跳过: {}, 清理: {}",
                knowledgeBaseId,
                processedNotes,
                totalNotes,
                action,
                note.getId(),
                getSafeTitle(note),
                result.getInsertedCount(),
                result.getUpdatedCount(),
                result.getSkippedCount(),
                result.getDeletedCount()
        );
    }

    private String getSafeTitle(Note note) {
        if (note == null || note.getTitle() == null || note.getTitle().isBlank()) {
            return "未命名笔记";
        }
        return note.getTitle();
    }

    private List<Note> listNotesByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getKnowledgeBaseId, knowledgeBaseId);
        return noteMapper.selectList(wrapper);
    }

    private void cleanupDeletedNoteIndexes(Set<Long> noteIdsToDelete) {
        for (Long noteId : noteIdsToDelete) {
            removeNoteIndexes(noteId);
            log.info("清理已删除笔记的索引，笔记ID: {}", noteId);
        }
    }

    private void removeNoteIndexes(Long noteId) {
        LambdaQueryWrapper<NoteVectorIndex> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(NoteVectorIndex::getNoteId, noteId);
        noteVectorIndexService.remove(deleteWrapper);
        qdrantClient.deleteByNoteId(noteId);
    }

    private void updateKnowledgeBaseIndexTime(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase != null) {
            knowledgeBase.setIndexUpdateTime(LocalDateTime.now());
            knowledgeBaseMapper.updateById(knowledgeBase);
        }
    }
}
