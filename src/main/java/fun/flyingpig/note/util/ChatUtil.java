package fun.flyingpig.note.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.flyingpig.note.config.ChatProperties;
import fun.flyingpig.note.dto.RagQueryDTO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 负责与对话模型交互，提供问题改写、普通回答和流式回答能力。
 */
@Component
@Slf4j
public class ChatUtil {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final Set<String> SUPPORTED_HISTORY_ROLES = Set.of("user", "assistant");
    private static final String GENERIC_ERROR_MESSAGE = "抱歉，生成回答时出现错误，请稍后重试。";

    private final ChatProperties chatProperties;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public ChatUtil(ChatProperties chatProperties) {
        this.chatProperties = chatProperties;
    }

    /**
     * 将多轮对话中的追问改写成可独立检索的完整问题。
     */
    public String rewriteQuestion(String question, List<RagQueryDTO.HistoryMessage> history) {
        if (!StringUtils.hasText(question) || history == null || history.isEmpty()) {
            return question;
        }

        JSONArray messages = new JSONArray();
        messages.add(buildMessage(
                "system",
                "你是一个查询改写助手。请结合历史对话，把用户最后一个问题改写成可以单独检索的完整问题。"
                        + "保留原始语言、专有名词和约束条件。如果原问题已经完整，原样返回。"
                        + "不要回答问题，不要解释，不要输出多余内容。"
        ));
        appendHistoryMessages(messages, history);
        messages.add(buildMessage("user", question));

        String rewrittenQuestion = callChatCompletion(messages, 0.1, 256);
        if (!StringUtils.hasText(rewrittenQuestion)) {
            return question;
        }
        return rewrittenQuestion.trim();
    }

    /**
     * 基于最近几轮消息和检索上下文生成最终回答。
     */
    public String generateAnswer(
            String question,
            String standaloneQuestion,
            String context,
            List<RagQueryDTO.HistoryMessage> history
    ) {
        JSONArray messages = buildAnswerMessages(question, standaloneQuestion, context, history);
        String answer = callChatCompletion(messages, 0.7, 8192);
        return StringUtils.hasText(answer) ? answer : GENERIC_ERROR_MESSAGE;
    }

    /**
     * 基于最近几轮消息和检索上下文流式生成回答。
     */
    public String streamAnswer(
            String question,
            String standaloneQuestion,
            String context,
            List<RagQueryDTO.HistoryMessage> history,
            Consumer<String> deltaConsumer
    ) {
        JSONArray messages = buildAnswerMessages(question, standaloneQuestion, context, history);
        Consumer<String> safeDeltaConsumer = deltaConsumer != null ? deltaConsumer : chunk -> { };
        String answer = streamChatCompletion(messages, 0.7, 8192, safeDeltaConsumer);
        if (StringUtils.hasText(answer)) {
            return answer;
        }

        safeDeltaConsumer.accept(GENERIC_ERROR_MESSAGE);
        return GENERIC_ERROR_MESSAGE;
    }

    private JSONArray buildAnswerMessages(
            String question,
            String standaloneQuestion,
            String context,
            List<RagQueryDTO.HistoryMessage> history
    ) {
        JSONArray messages = new JSONArray();
        messages.add(buildMessage(
                "system",
                "你是一个知识库问答助手。请优先依据检索到的知识库内容回答当前问题，回答要准确、简洁、有条理。"
                        + "如果知识库上下文不足以支撑结论，要明确说明，不要编造。"
        ));

        appendHistoryMessages(messages, history);

        StringBuilder retrievalContext = new StringBuilder();
        retrievalContext.append("以下是本轮检索到的知识库上下文，请结合这些内容回答。");
        retrievalContext.append("\n如果上下文与问题无关或不足，请直接说明。");
        if (StringUtils.hasText(standaloneQuestion)) {
            retrievalContext.append("\n\n【检索使用的独立问题】\n").append(standaloneQuestion.trim());
        }
        retrievalContext.append("\n\n【知识库上下文】\n").append(context);
        messages.add(buildMessage("system", retrievalContext.toString()));

        messages.add(buildMessage("user", question));
        return messages;
    }

    private String callChatCompletion(JSONArray messages, double temperature, int maxTokens) {
        long startTime = System.currentTimeMillis();
        try {
            JSONObject requestBody = buildRequestBody(messages, temperature, maxTokens, false);
            Request request = buildChatCompletionRequest(requestBody, false);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    log.error("Chat completion request failed: code={}, body={}", response.code(), responseBody);
                    return null;
                }

                if (response.body() == null) {
                    log.error("Chat completion response body is empty");
                    return null;
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    log.info("大模型非流式接口调用完成，耗时: {}ms", System.currentTimeMillis() - startTime);
                    return choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                }

                log.error("Chat completion response has no choices: {}", responseBody);
            }
        } catch (IOException e) {
            log.error("Chat completion request failed", e);
        }
        return null;
    }

    private String streamChatCompletion(
            JSONArray messages,
            double temperature,
            int maxTokens,
            Consumer<String> deltaConsumer
    ) {
        long startTime = System.currentTimeMillis();
        try {
            JSONObject requestBody = buildRequestBody(messages, temperature, maxTokens, true);
            Request request = buildChatCompletionRequest(requestBody, true);

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    log.error("Stream chat completion request failed: code={}, body={}", response.code(), responseBody);
                    return null;
                }

                if (response.body() == null) {
                    log.error("Stream chat completion response body is empty");
                    return null;
                }

                StringBuilder fullAnswer = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8)
                )) {
                    String line;
                    StringBuilder eventData = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            if (handleStreamEvent(eventData, fullAnswer, deltaConsumer)) {
                                break;
                            }
                            eventData.setLength(0);
                            continue;
                        }

                        if (line.startsWith("data:")) {
                            if (eventData.length() > 0) {
                                eventData.append('\n');
                            }
                            eventData.append(line.substring(5).trim());
                        }
                    }

                    if (eventData.length() > 0) {
                        handleStreamEvent(eventData, fullAnswer, deltaConsumer);
                    }
                }

                log.info("大模型流式接口调用完成，耗时: {}ms", System.currentTimeMillis() - startTime);
                return fullAnswer.toString();
            }
        } catch (IOException e) {
            log.error("Stream chat completion request failed", e);
        }
        return null;
    }

    private boolean handleStreamEvent(
            StringBuilder eventData,
            StringBuilder fullAnswer,
            Consumer<String> deltaConsumer
    ) {
        String data = eventData.toString().trim();
        if (!StringUtils.hasText(data)) {
            return false;
        }

        if ("[DONE]".equals(data)) {
            return true;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(data);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return false;
            }

            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            if (delta == null) {
                return false;
            }

            String content = delta.getString("content");
            if (StringUtils.hasText(content)) {
                fullAnswer.append(content);
                deltaConsumer.accept(content);
            }
        } catch (Exception e) {
            log.warn("Failed to parse stream event: {}", data, e);
        }
        return false;
    }

    private JSONObject buildRequestBody(JSONArray messages, double temperature, int maxTokens, boolean stream) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", chatProperties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        if (stream) {
            requestBody.put("stream", true);
        }
        return requestBody;
    }

    private Request buildChatCompletionRequest(JSONObject requestBody, boolean stream) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(chatProperties.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + chatProperties.getApiKey())
                .header("Content-Type", "application/json");

        if (stream) {
            requestBuilder.header("Accept", "text/event-stream");
        }

        return requestBuilder
                .post(RequestBody.create(requestBody.toJSONString(), JSON_MEDIA_TYPE))
                .build();
    }

    private void appendHistoryMessages(JSONArray messages, List<RagQueryDTO.HistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return;
        }

        for (RagQueryDTO.HistoryMessage historyMessage : history) {
            if (historyMessage == null || !StringUtils.hasText(historyMessage.getContent())) {
                continue;
            }

            String role = normalizeRole(historyMessage.getRole());
            if (!SUPPORTED_HISTORY_ROLES.contains(role)) {
                continue;
            }

            messages.add(buildMessage(role, historyMessage.getContent().trim()));
        }
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private JSONObject buildMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
