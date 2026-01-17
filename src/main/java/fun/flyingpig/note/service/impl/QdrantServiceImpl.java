package fun.flyingpig.note.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.flyingpig.note.config.QdrantProperties;
import fun.flyingpig.note.dto.QdrantSearchResult;
import fun.flyingpig.note.entity.QdrantPoint;
import fun.flyingpig.note.service.QdrantService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Qdrant向量数据库服务实现类
 */
@Slf4j
@Service
public class QdrantServiceImpl implements QdrantService {

    @Autowired
    private QdrantProperties qdrantProperties;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        // 初始化时检查并创建Collection
        initCollection();
    }

    @Override
    public void initCollection() {
        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName();

        try {
            // 先检查Collection是否存在
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("Qdrant Collection '{}' 已存在", qdrantProperties.getCollectionName());
        } catch (HttpClientErrorException.NotFound e) {
            // Collection不存在，创建它
            createCollection();
        } catch (Exception e) {
            log.error("检查Qdrant Collection时出错: {}", e.getMessage());
        }
    }

    private void createCollection() {
        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName();

        JSONObject body = new JSONObject();
        JSONObject vectors = new JSONObject();
        vectors.put("size", qdrantProperties.getVectorSize());
        vectors.put("distance", qdrantProperties.getDistance());
        body.put("vectors", vectors);

        HttpHeaders headers = getHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.info("成功创建Qdrant Collection: {}", qdrantProperties.getCollectionName());

            // 创建payload索引以加速过滤查询
            createPayloadIndex("note_id", "integer");
            createPayloadIndex("knowledge_base_id", "integer");
        } catch (Exception e) {
            log.error("创建Qdrant Collection失败: {}", e.getMessage());
            throw new RuntimeException("创建Qdrant Collection失败", e);
        }
    }

    private void createPayloadIndex(String fieldName, String fieldType) {
        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName()
                + "/index";

        JSONObject body = new JSONObject();
        body.put("field_name", fieldName);
        body.put("field_schema", fieldType);

        HttpHeaders headers = getHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.info("成功创建Qdrant索引: {}", fieldName);
        } catch (Exception e) {
            log.warn("创建Qdrant索引失败(可能已存在): {} - {}", fieldName, e.getMessage());
        }
    }

    @Override
    public void upsertPoint(Long pointId, List<Double> vector, Long noteId, Long knowledgeBaseId,
                            Integer chunkIndex, String chunkContent) {
        QdrantPoint point = new QdrantPoint(pointId, vector, noteId, knowledgeBaseId, chunkIndex, chunkContent);
        upsertPoints(Collections.singletonList(point));
    }

    @Override
    public void upsertPoints(List<QdrantPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }

        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName()
                + "/points?wait=true";

        JSONArray pointsArray = new JSONArray();
        for (QdrantPoint point : points) {
            pointsArray.add(convertPointToJsonObject(point));
        }

        JSONObject body = new JSONObject();
        body.put("points", pointsArray);

        HttpHeaders headers = getHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.debug("成功写入 {} 个点到Qdrant", points.size());
        } catch (Exception e) {
            log.error("写入Qdrant失败: {}", e.getMessage());
            throw new RuntimeException("写入Qdrant失败", e);
        }
    }

    private JSONObject convertPointToJsonObject(QdrantPoint point) {
        JSONObject pointJson = new JSONObject();
        pointJson.put("id", point.getId());

        // 转换 List<Double> 为 float数组
        JSONArray vectorArray = new JSONArray();
        for (Double val : point.getVector()) {
            vectorArray.add(val.floatValue());
        }
        pointJson.put("vector", vectorArray);

        // Payload
        JSONObject payload = new JSONObject();
        payload.put("note_id", point.getNoteId());
        payload.put("knowledge_base_id", point.getKnowledgeBaseId());
        payload.put("chunk_index", point.getChunkIndex());
        payload.put("chunk_content", point.getChunkContent());
        pointJson.put("payload", payload);

        return pointJson;
    }

    @Override
    public void deleteByNoteId(Long noteId) {
        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName()
                + "/points/delete?wait=true";

        JSONObject body = new JSONObject();
        JSONObject filter = new JSONObject();
        JSONObject must = new JSONObject();
        JSONObject match = new JSONObject();
        match.put("value", noteId);
        must.put("key", "note_id");
        must.put("match", match);
        filter.put("must", Collections.singletonList(must));
        body.put("filter", filter);

        HttpHeaders headers = getHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.debug("成功从Qdrant删除笔记ID={}的所有向量", noteId);
        } catch (Exception e) {
            log.error("从Qdrant按笔记ID删除失败: {}", e.getMessage());
        }
    }

    @Override
    public void deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName()
                + "/points/delete?wait=true";

        JSONObject body = new JSONObject();
        JSONObject filter = new JSONObject();
        JSONObject must = new JSONObject();
        JSONObject match = new JSONObject();
        match.put("value", knowledgeBaseId);
        must.put("key", "knowledge_base_id");
        must.put("match", match);
        filter.put("must", Collections.singletonList(must));
        body.put("filter", filter);

        HttpHeaders headers = getHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("成功从Qdrant删除知识库ID={}的所有向量", knowledgeBaseId);
        } catch (Exception e) {
            log.error("从Qdrant按知识库ID删除失败: {}", e.getMessage());
        }
    }

    @Override
    public List<QdrantSearchResult> search(List<Double> queryVector, List<Long> knowledgeBaseIds, long topK) {
        String url = qdrantProperties.getHost() + "/collections/" + qdrantProperties.getCollectionName()
                + "/points/search";

        JSONObject body = new JSONObject();

        // 转换查询向量
        body.put("vector", convertVectorToJsonArray(queryVector));
        body.put("limit", topK);
        body.put("with_payload", true);

        // 构建知识库ID过滤条件
        if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
            body.put("filter", buildKnowledgeBaseFilter(knowledgeBaseIds));
        }

        HttpHeaders headers = getHeaders();
        HttpEntity<String> request = new HttpEntity<>(body.toJSONString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return parseSearchResults(response.getBody());
        } catch (Exception e) {
            log.error("Qdrant搜索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private JSONArray convertVectorToJsonArray(List<Double> vector) {
        JSONArray vectorArray = new JSONArray();
        for (Double val : vector) {
            vectorArray.add(val.floatValue());
        }
        return vectorArray;
    }

    private JSONObject buildKnowledgeBaseFilter(List<Long> knowledgeBaseIds) {
        JSONObject filter = new JSONObject();

        JSONArray shouldArray = new JSONArray();
        for (Long kbId : knowledgeBaseIds) {
            JSONObject condition = new JSONObject();
            condition.put("key", "knowledge_base_id");
            JSONObject match = new JSONObject();
            match.put("value", kbId);
            condition.put("match", match);
            shouldArray.add(condition);
        }
        filter.put("should", shouldArray);

        return filter;
    }

    private List<QdrantSearchResult> parseSearchResults(String responseBody) {
        List<QdrantSearchResult> results = new ArrayList<>();

        try {
            JSONObject response = JSON.parseObject(responseBody);
            JSONArray resultArray = response.getJSONArray("result");

            if (resultArray != null) {
                for (int i = 0; i < resultArray.size(); i++) {
                    JSONObject item = resultArray.getJSONObject(i);
                    results.add(parseSingleResult(item));
                }
            }
        } catch (Exception e) {
            log.error("解析Qdrant搜索结果失败: {}", e.getMessage());
        }

        return results;
    }

    private QdrantSearchResult parseSingleResult(JSONObject item) {
        QdrantSearchResult result = new QdrantSearchResult();

        result.setId(item.getLong("id"));
        result.setScore(item.getDouble("score"));

        JSONObject payload = item.getJSONObject("payload");
        if (payload != null) {
            result.setNoteId(payload.getLong("note_id"));
            result.setKnowledgeBaseId(payload.getLong("knowledge_base_id"));
            result.setChunkIndex(payload.getInteger("chunk_index"));
            result.setChunkContent(payload.getString("chunk_content"));
        }

        return result;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (qdrantProperties.getApiKey() != null && !qdrantProperties.getApiKey().isEmpty()) {
            headers.set("api-key", qdrantProperties.getApiKey());
        }
        return headers;
    }
}