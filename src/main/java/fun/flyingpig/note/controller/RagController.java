package fun.flyingpig.note.controller;

import com.alibaba.fastjson.JSON;
import fun.flyingpig.note.dto.RagAnswerDTO;
import fun.flyingpig.note.dto.RagQueryDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.dto.UpdateIndexDTO;
import fun.flyingpig.note.dto.UpdateIndexResultDTO;
import fun.flyingpig.note.service.RagService;
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

    private final RagService ragService;

    @PostMapping("/answer")
    public Result<RagAnswerDTO> answer(@RequestBody @Validated RagQueryDTO queryDTO) {
        log.info("收到RAG问答请求: {}", queryDTO.getQuestion());
        RagAnswerDTO answer = ragService.answer(queryDTO);
        return Result.success(answer);
    }

    @PostMapping(value = "/answer/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody answerStream(
            @RequestBody @Validated RagQueryDTO queryDTO,
            HttpServletResponse response
    ) {
        log.info("收到RAG流式问答请求: {}", queryDTO.getQuestion());
        prepareSseResponse(response);

        return outputStream -> {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
            try {
                RagAnswerDTO answer = ragService.answerStream(queryDTO, delta ->
                        writeSseEvent(writer, "delta", Map.of("content", delta))
                );

                Map<String, Object> donePayload = new LinkedHashMap<>();
                donePayload.put("answer", answer.getAnswer());
                donePayload.put("relevantDocuments", answer.getRelevantDocuments());
                writeSseEvent(writer, "done", donePayload);
            } catch (Exception e) {
                log.error("RAG流式问答失败", e);
                writeSseEvent(writer, "error", Map.of("message", "抱歉，流式回答生成失败，请稍后重试。"));
            } finally {
                writer.flush();
            }
        };
    }

    @PostMapping("/updateIndex")
    public Result<UpdateIndexResultDTO> updateIndex(@RequestBody @Validated UpdateIndexDTO updateIndexDTO) {
        log.info("收到更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        UpdateIndexResultDTO result = ragService.updateIndex(updateIndexDTO);
        return Result.success(result);
    }

    @PostMapping(value = "/updateIndex/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody updateIndexStream(
            @RequestBody @Validated UpdateIndexDTO updateIndexDTO,
            HttpServletResponse response
    ) {
        log.info("收到流式更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        prepareSseResponse(response);
        return streamIndexUpdate(
                updateIndexDTO,
                writer -> ragService.updateIndex(updateIndexDTO, progress -> writeSseEvent(writer, "progress", progress))
        );
    }

    @PostMapping("/forceUpdateIndex")
    public Result<UpdateIndexResultDTO> forceUpdateIndex(@RequestBody @Validated UpdateIndexDTO updateIndexDTO) {
        log.info("收到强制更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        UpdateIndexResultDTO result = ragService.forceUpdateIndex(updateIndexDTO);
        return Result.success(result);
    }

    @PostMapping(value = "/forceUpdateIndex/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public StreamingResponseBody forceUpdateIndexStream(
            @RequestBody @Validated UpdateIndexDTO updateIndexDTO,
            HttpServletResponse response
    ) {
        log.info("收到流式强制更新索引请求, 知识库ID: {}", updateIndexDTO.getKnowledgeBaseId());
        prepareSseResponse(response);
        return streamIndexUpdate(
                updateIndexDTO,
                writer -> ragService.forceUpdateIndex(updateIndexDTO, progress -> writeSseEvent(writer, "progress", progress))
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
                writeSseEvent(writer, "error", Map.of("message", "索引更新失败，请稍后重试。"));
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
            throw new RuntimeException("写入SSE事件失败", e);
        }
    }
}
