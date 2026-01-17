package fun.flyingpig.note.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.flyingpig.note.config.RagProperties;
import fun.flyingpig.note.dto.*;
import fun.flyingpig.note.entity.KnowledgeBase;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteVectorIndex;
import fun.flyingpig.note.entity.QdrantPoint;
import fun.flyingpig.note.mapper.KnowledgeBaseMapper;
import fun.flyingpig.note.mapper.NoteMapper;
import fun.flyingpig.note.service.INoteVectorIndexService;
import fun.flyingpig.note.service.QdrantService;
import fun.flyingpig.note.service.RagService;
import fun.flyingpig.note.util.ChatUtil;
import fun.flyingpig.note.util.MarkdownChunker;
import fun.flyingpig.note.util.embedding.ZhiPuEmbedding3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    @Autowired
    ZhiPuEmbedding3Util zhiPuEmbedding3Util;

    @Autowired
    RagProperties ragProperties;

    @Autowired
    ChatUtil chatUtil;

    @Autowired
    QdrantService qdrantService;

    private final NoteMapper noteMapper;
    private final INoteVectorIndexService noteVectorIndexService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public RagAnswerDTO answer(RagQueryDTO queryDTO) {
        log.info("RAG查询开始, 问题: {}, 知识库IDs: {}", queryDTO.getQuestion(), queryDTO.getKnowledgeBaseIds());

        long questionEmbeddingStartTime = System.currentTimeMillis();

        // 获取问题的向量嵌入
        List<Double> queryEmbedding = zhiPuEmbedding3Util.getEmbedding(queryDTO.getQuestion());
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            throw new RuntimeException("获取问题向量失败");
        }

        log.info("请求API将问题转化为向量耗时: {}ms", System.currentTimeMillis() - questionEmbeddingStartTime);

        // ========== 使用Qdrant进行向量搜索 ==========
        long qdrantSearchStartTime = System.currentTimeMillis();

        List<QdrantSearchResult> searchResults = qdrantService.search(
                queryEmbedding,
                queryDTO.getKnowledgeBaseIds(),
                ragProperties.getTopK()
        );

        if (searchResults.isEmpty()) {
            RagAnswerDTO result = new RagAnswerDTO();
            result.setAnswer("抱歉，指定的知识库中暂无索引数据，请先更新索引。");
            result.setRelevantDocuments(new ArrayList<>());
            return result;
        }

        log.info("Qdrant向量搜索耗时: {}ms, 找到{}条结果",
                System.currentTimeMillis() - qdrantSearchStartTime, searchResults.size());

        // 获取笔记信息构建上下文
        long contextStartTime = System.currentTimeMillis();

        Set<Long> noteIds = searchResults.stream()
                .map(QdrantSearchResult::getNoteId)
                .collect(Collectors.toSet());
        Map<Long, Note> noteMap = new HashMap<>();
        if (!noteIds.isEmpty()) {
            List<Note> notes = noteMapper.selectBatchIds(noteIds);
            noteMap = notes.stream().collect(Collectors.toMap(Note::getId, n -> n));
        }

        StringBuilder contextBuilder = new StringBuilder();
        List<RagAnswerDTO.RelevantDocument> relevantDocs = new ArrayList<>();

        for (QdrantSearchResult searchResult : searchResults) {
            Note note = noteMap.get(searchResult.getNoteId());

            contextBuilder.append("【来源: ").append(note != null ? note.getTitle() : "未知").append("】\n");
            contextBuilder.append(searchResult.getChunkContent()).append("\n\n");

            RagAnswerDTO.RelevantDocument doc = new RagAnswerDTO.RelevantDocument();
            doc.setNoteId(searchResult.getNoteId());
            doc.setNoteTitle(note != null ? note.getTitle() : "未知");
            doc.setContent(searchResult.getChunkContent());
            doc.setScore(searchResult.getScore());
            relevantDocs.add(doc);
        }

        log.info("构建上下文耗时: {}ms", System.currentTimeMillis() - contextStartTime);

        // 调用DeepSeek生成回答
        long requestStartTime = System.currentTimeMillis();

        String answer = chatUtil.generateAnswer(queryDTO.getQuestion(), contextBuilder.toString());

        log.info("请求回答耗时: {}ms", System.currentTimeMillis() - requestStartTime);

        RagAnswerDTO result = new RagAnswerDTO();
        result.setAnswer(answer);
        result.setRelevantDocuments(relevantDocs);

        log.info("RAG查询完成, 找到{}个相关文档, 总耗时: {}ms",
                relevantDocs.size(), System.currentTimeMillis() - questionEmbeddingStartTime);
        return result;
    }

    @Override
    @Transactional
    public UpdateIndexResultDTO updateIndex(UpdateIndexDTO updateIndexDTO) {
        Long kbId = updateIndexDTO.getKnowledgeBaseId();
        log.info("开始更新知识库索引, 知识库ID: {}", kbId);

        // 获取知识库下所有笔记
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getKnowledgeBaseId, kbId);
        List<Note> notes = noteMapper.selectList(wrapper);

        // 获取知识库下所有现有的向量索引
        List<NoteVectorIndex> existingIndexes = noteVectorIndexService.selectByKnowledgeBaseIds(Arrays.asList(kbId));

        // 收集现有索引对应的笔记ID
        Set<Long> indexedNoteIds = existingIndexes.stream()
                .map(NoteVectorIndex::getNoteId)
                .collect(Collectors.toSet());

        // 收集当前知识库中实际存在的笔记ID
        Set<Long> actualNoteIds = notes.stream()
                .map(Note::getId)
                .collect(Collectors.toSet());

        // 找出需要删除的索引（对应笔记已被删除）
        Set<Long> noteIdsToDelete = new HashSet<>(indexedNoteIds);
        noteIdsToDelete.removeAll(actualNoteIds);

        // 删除这些无效的索引（MySQL + Qdrant双删）
        for (Long noteId : noteIdsToDelete) {
            // 删除MySQL中的索引
            LambdaQueryWrapper<NoteVectorIndex> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(NoteVectorIndex::getNoteId, noteId);
            noteVectorIndexService.remove(deleteWrapper);
            // 同步删除Qdrant中的索引
            qdrantService.deleteByNoteId(noteId);
            log.info("清理了已删除笔记的索引, 笔记ID: {}", noteId);
        }

        UpdateIndexResultDTO result = new UpdateIndexResultDTO();
        result.setKnowledgeBaseId(kbId);
        result.setInsertedCount(0);
        result.setUpdatedCount(0);
        result.setSkippedCount(0);
        result.setDeletedCount(noteIdsToDelete.size());
        List<UpdateIndexResultDTO.NoteIndexDetail> details = new ArrayList<>();

        for (Note note : notes) {
            UpdateIndexResultDTO.NoteIndexDetail detail = new UpdateIndexResultDTO.NoteIndexDetail();
            detail.setNoteId(note.getId());
            detail.setNoteTitle(note.getTitle());

            // 检查是否存在索引
            LocalDateTime latestIndexTime = noteVectorIndexService.getLatestUpdateTimeByNoteId(note.getId());

            if (latestIndexTime == null) {
                // 不存在索引，执行插入
                createIndexForNote(note);
                detail.setAction("INSERT");
                detail.setMessage("新建索引成功");
                result.setInsertedCount(result.getInsertedCount() + 1);
            } else if (latestIndexTime.isBefore(note.getUpdateTime())) {
                // 索引过期，需要更新
                // 先删除旧索引（MySQL + Qdrant）
                LambdaQueryWrapper<NoteVectorIndex> deleteWrapper = new LambdaQueryWrapper<>();
                deleteWrapper.eq(NoteVectorIndex::getNoteId, note.getId());
                noteVectorIndexService.remove(deleteWrapper);
                qdrantService.deleteByNoteId(note.getId());

                // 重新创建索引
                createIndexForNote(note);
                detail.setAction("UPDATE");
                detail.setMessage("索引已更新");
                result.setUpdatedCount(result.getUpdatedCount() + 1);
            } else {
                // 索引是最新的，跳过
                detail.setAction("SKIP");
                detail.setMessage("索引已是最新");
                result.setSkippedCount(result.getSkippedCount() + 1);
            }

            details.add(detail);
        }

        result.setDetails(details);

        // 更新知识库的索引更新时间
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbId);
        if (knowledgeBase != null) {
            knowledgeBase.setIndexUpdateTime(LocalDateTime.now());
            knowledgeBaseMapper.updateById(knowledgeBase);
        }

        log.info("知识库索引更新完成, 新增: {}, 更新: {}, 跳过: {}, 清理: {}",
                result.getInsertedCount(), result.getUpdatedCount(), result.getSkippedCount(), result.getDeletedCount());
        return result;
    }

    @Override
    @Transactional
    public UpdateIndexResultDTO forceUpdateIndex(UpdateIndexDTO updateIndexDTO) {
        Long kbId = updateIndexDTO.getKnowledgeBaseId();
        log.info("开始强制更新知识库索引, 知识库ID: {}", kbId);

        // 获取知识库下所有笔记
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getKnowledgeBaseId, kbId);
        List<Note> notes = noteMapper.selectList(wrapper);

        // 删除该知识库下的所有现有索引（MySQL + Qdrant双删）
        noteVectorIndexService.deleteByKnowledgeBaseId(kbId);
        qdrantService.deleteByKnowledgeBaseId(kbId);
        log.info("已删除知识库 {} 的所有现有索引(MySQL + Qdrant)", kbId);

        UpdateIndexResultDTO result = new UpdateIndexResultDTO();
        result.setKnowledgeBaseId(kbId);
        result.setInsertedCount(notes.size());
        result.setUpdatedCount(0);
        result.setSkippedCount(0);
        result.setDeletedCount(0);
        List<UpdateIndexResultDTO.NoteIndexDetail> details = new ArrayList<>();

        // 为所有笔记重新创建索引
        for (Note note : notes) {
            UpdateIndexResultDTO.NoteIndexDetail detail = new UpdateIndexResultDTO.NoteIndexDetail();
            detail.setNoteId(note.getId());
            detail.setNoteTitle(note.getTitle());

            // 为笔记创建向量索引
            createIndexForNote(note);

            detail.setAction("INSERT");
            detail.setMessage("强制重建索引成功");
            details.add(detail);
        }

        result.setDetails(details);

        // 更新知识库的索引更新时间
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(kbId);
        if (knowledgeBase != null) {
            knowledgeBase.setIndexUpdateTime(LocalDateTime.now());
            knowledgeBaseMapper.updateById(knowledgeBase);
        }

        log.info("知识库索引强制更新完成, 重建了 {} 个笔记的索引", notes.size());
        return result;
    }

    /**
     * 为笔记创建向量索引
     * 同时写入MySQL和Qdrant（双写）
     */
    private void createIndexForNote(Note note) {
        String content = note.getContent();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        // 分块处理
        List<String> chunks = MarkdownChunker.split(content);

        // 用于批量写入Qdrant的点列表
        List<QdrantPoint> qdrantPoints = new ArrayList<>();
        List<NoteVectorIndex> noteVectorIndices = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            List<Double> embedding = zhiPuEmbedding3Util.getEmbedding(chunk);

            if (embedding != null && !embedding.isEmpty()) {
                // 写入MySQL的数据
                NoteVectorIndex index = new NoteVectorIndex();
                index.setNoteId(note.getId());
                index.setKnowledgeBaseId(note.getKnowledgeBaseId());
                index.setChunkIndex(i);
                index.setChunkContent(chunk);
                index.setEmbedding(JSON.toJSONString(embedding));
                index.setCreateTime(LocalDateTime.now());
                index.setUpdateTime(LocalDateTime.now());
                noteVectorIndices.add(index);
                // 写入Qdrant的数据
                QdrantPoint point = new QdrantPoint();
                point.setVector(embedding);
                point.setNoteId(note.getId());
                point.setKnowledgeBaseId(note.getKnowledgeBaseId());
                point.setChunkIndex(i);
                point.setChunkContent(chunk);
                qdrantPoints.add(point);
            }
        }
        // 批量写入Mysql
        noteVectorIndexService.saveBatch(noteVectorIndices);

        for (int i = 0; i < qdrantPoints.size(); i++) {
            qdrantPoints.get(i).setId(noteVectorIndices.get(i).getId());
        }
        // 批量写入Qdrant
        if (!qdrantPoints.isEmpty()) {
            try {
                qdrantService.upsertPoints(qdrantPoints);
                log.debug("笔记 {} 的 {} 个分块已同步写入Qdrant", note.getId(), qdrantPoints.size());
            } catch (Exception e) {
                log.error("写入Qdrant失败，笔记ID: {}, 错误: {}", note.getId(), e.getMessage());
            }
        }
    }
}
