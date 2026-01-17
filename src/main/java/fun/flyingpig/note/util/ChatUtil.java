package fun.flyingpig.note.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import fun.flyingpig.note.config.ChatProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class ChatUtil {

    @Autowired
    ChatProperties chatProperties;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    /**
     * 使用DeepSeek生成回答
     */
    public String generateAnswer(String question, String context) {
        try {
            String systemPrompt = "你是一个文档库智能助手，可以基于从知识库检索到的内容回答用户问题，回答要准确、简洁、有条理。";

            String userPrompt = "请基于以下从知识库中检索的上下文内容回答问题：\n\n" +
                    "【知识库内容】\n" + context + "\n\n" +
                    "【用户问题】\n" + question;

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", chatProperties.getModel());

            JSONArray messages = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 8192);

            Request request = new Request.Builder()
                    .url(chatProperties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + chatProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toJSONString(), JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API调用失败: {}", response.code());
                    return "抱歉，生成回答时出现错误，请稍后重试。";
                }

                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    return choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                }
            }
        } catch (IOException e) {
            log.error("生成回答失败", e);
        }
        return "抱歉，生成回答时出现错误，请稍后重试。";
    }
}
