package fun.flyingpig.note.service.index;

import com.alibaba.fastjson.JSON;
import fun.flyingpig.note.entity.Note;
import fun.flyingpig.note.entity.NoteVectorIndex;
import fun.flyingpig.note.entity.QdrantPoint;
import fun.flyingpig.note.service.INoteVectorIndexService;
import fun.flyingpig.note.service.qdrant.QdrantService;
import fun.flyingpig.note.util.MarkdownChunker;
import fun.flyingpig.note.util.embedding.ZhiPuEmbedding3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteIndexWriter {

    private final ZhiPuEmbedding3Util zhiPuEmbedding3Util;
    private final INoteVectorIndexService noteVectorIndexService;
    private final QdrantService qdrantService;

    public void writeIndexForNote(Note note) {
        long startTime = System.currentTimeMillis();
        String content = note.getContent();
        if (content == null || content.trim().isEmpty()) {
            log.info("跳过空笔记索引写入，笔记ID: {}, 标题: {}", note.getId(), getSafeTitle(note));
            return;
        }

        List<String> chunks = MarkdownChunker.split(content);
        log.info("开始写入笔记索引，笔记ID: {}, 标题: {}, 分块数: {}", note.getId(), getSafeTitle(note), chunks.size());
        List<QdrantPoint> qdrantPoints = new ArrayList<>();
        List<NoteVectorIndex> noteVectorIndices = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            List<Double> embedding = zhiPuEmbedding3Util.getEmbedding(chunk);

            if (embedding != null && !embedding.isEmpty()) {
                NoteVectorIndex index = new NoteVectorIndex();
                index.setNoteId(note.getId());
                index.setKnowledgeBaseId(note.getKnowledgeBaseId());
                index.setChunkIndex(i);
                index.setChunkContent(chunk);
                index.setEmbedding(JSON.toJSONString(embedding));
                index.setCreateTime(LocalDateTime.now());
                index.setUpdateTime(LocalDateTime.now());
                noteVectorIndices.add(index);

                QdrantPoint point = new QdrantPoint();
                point.setVector(embedding);
                point.setNoteId(note.getId());
                point.setKnowledgeBaseId(note.getKnowledgeBaseId());
                point.setChunkIndex(i);
                point.setChunkContent(chunk);
                qdrantPoints.add(point);
            }
        }

        noteVectorIndexService.saveBatch(noteVectorIndices);

        for (int i = 0; i < qdrantPoints.size(); i++) {
            qdrantPoints.get(i).setId(noteVectorIndices.get(i).getId());
        }

        if (!qdrantPoints.isEmpty()) {
            try {
                qdrantService.upsertPoints(qdrantPoints);
                log.debug("笔记 {} 的 {} 个分块已同步写入Qdrant", note.getId(), qdrantPoints.size());
            } catch (Exception e) {
                log.error("写入Qdrant失败，笔记ID: {}, 标题: {}, 错误: {}", note.getId(), getSafeTitle(note), e.getMessage());
                throw new RuntimeException("写入Qdrant失败，已中止索引更新", e);
            }
        }

        log.info(
                "笔记索引写入完成，笔记ID: {}, 标题: {}, 分块数: {}, 有效向量数: {}, 耗时: {}ms",
                note.getId(),
                getSafeTitle(note),
                chunks.size(),
                qdrantPoints.size(),
                System.currentTimeMillis() - startTime
        );
    }

    private String getSafeTitle(Note note) {
        if (note == null || note.getTitle() == null || note.getTitle().isBlank()) {
            return "未命名笔记";
        }
        return note.getTitle();
    }
}
