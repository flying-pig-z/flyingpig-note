package fun.flyingpig.note.controller;

import com.alibaba.fastjson.JSON;
import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;
import fun.flyingpig.note.service.rag.RagAnswerService;
import fun.flyingpig.note.service.rag.RagIndexService;
import fun.flyingpig.note.service.security.NoteSecurityService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagAnswerService ragAnswerService;
    private final RagIndexService ragIndexService;
    private final NoteSecurityService noteSecurityService;

    @PostMapping("/answer")
    public Result<RagAnswerDTO> answer(@RequestBody @Validated RagQueryDTO queryDTO) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteSecurityService.requireKnowledgeBaseOwnership(queryDTO.getKnowledgeBaseIds(), userId);
        log.info("收到 RAG 问答请求: {}", queryDTO.getQuestion());
        return Result.success(ragAnswerService.answer(queryDTO));
    }

    @PostMapping(value = "/answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody answerStream(
            @RequestBody @Validated RagQueryDTO queryDTO,
            HttpServletResponse response
    ) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteSecurityService.requireKnowledgeBaseOwnership(queryDTO.getKnowledgeBaseIds(), userId);
        log.info("收到 RAG 流式问答请求: {}", queryDTO.getQuestion());
        prepareSseResponse(response);

        return outputStream -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            try {
                RagAnswerDTO answer = ragAnswerService.answerStream(queryDTO, delta ->
                        writeSseEvent(writer, "delta", Map.of("content", delta))
                );

                Map<String, Object> donePayload = new LinkedHashMap<>();
                donePayload.put("answer", answer.getAnswer());
                donePayload.put("relevantDocuments", answer.getRelevantDocuments());
                writeSseEvent(writer, "done", donePayload);
            } catch (Exception e) {
                log.error("RAG 流式问答失败", e);
                writeSseEvent(writer, "error", Map.of("message", "流式回答生成失败，请稍后重试"));
            } finally {
                writer.flush();
            }
        };
    }

    @PostMapping("/updateIndex")
    public Result<UpdateIndexResultDTO> updateIndex(@RequestBody @Validated UpdateIndexDTO updateIndexDTO) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteSecurityService.requireKnowledgeBaseOwner(updateIndexDTO.getKnowledgeBaseId(), userId);
        log.info("收到更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        return Result.success(ragIndexService.updateIndex(updateIndexDTO));
    }

    @PostMapping(value = "/updateIndex/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody updateIndexStream(
            @RequestBody @Validated UpdateIndexDTO updateIndexDTO,
            HttpServletResponse response
    ) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteSecurityService.requireKnowledgeBaseOwner(updateIndexDTO.getKnowledgeBaseId(), userId);
        log.info("收到流式更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        prepareSseResponse(response);
        return streamIndexUpdate(
                updateIndexDTO,
                writer -> ragIndexService.updateIndex(updateIndexDTO, progress -> writeSseEvent(writer, "progress", progress))
        );
    }

    @PostMapping("/forceUpdateIndex")
    public Result<UpdateIndexResultDTO> forceUpdateIndex(@RequestBody @Validated UpdateIndexDTO updateIndexDTO) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteSecurityService.requireKnowledgeBaseOwner(updateIndexDTO.getKnowledgeBaseId(), userId);
        log.info("收到强制更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        return Result.success(ragIndexService.forceUpdateIndex(updateIndexDTO));
    }

    @PostMapping(value = "/forceUpdateIndex/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody forceUpdateIndexStream(
            @RequestBody @Validated UpdateIndexDTO updateIndexDTO,
            HttpServletResponse response
    ) {
        Long userId = noteSecurityService.requireCurrentUserId();
        noteSecurityService.requireKnowledgeBaseOwner(updateIndexDTO.getKnowledgeBaseId(), userId);
        log.info("收到流式强制更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        prepareSseResponse(response);
        return streamIndexUpdate(
                updateIndexDTO,
                writer -> ragIndexService.forceUpdateIndex(updateIndexDTO, progress -> writeSseEvent(writer, "progress", progress))
        );
    }

    private StreamingResponseBody streamIndexUpdate(
            UpdateIndexDTO updateIndexDTO,
            Function<BufferedWriter, UpdateIndexResultDTO> executor
    ) {
        return outputStream -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            try {
                UpdateIndexResultDTO result = executor.apply(writer);
                writeSseEvent(writer, "done", result);
            } catch (Exception e) {
                log.error("索引更新流失败, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId(), e);
                writeSseEvent(writer, "error", Map.of("message", "索引更新失败，请稍后重试"));
            } finally {
                writer.flush();
            }
        };
    }

    private void prepareSseResponse(HttpServletResponse response) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private void writeSseEvent(BufferedWriter writer, String event, Object data) {
        try {
            writer.write("event: " + event);
            writer.newLine();
            writer.write("data: " + JSON.toJSONString(data));
            writer.newLine();
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException("写入 SSE 事件失败", e);
        }
    }
}
