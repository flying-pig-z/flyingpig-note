package fun.flyingpig.note.util.embedding;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.embedding.EmbeddingCreateParams;
import ai.z.openapi.service.embedding.EmbeddingResponse;
import fun.flyingpig.note.config.ZhipuProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZhiPuEmbedding3Util {

    private final ZhipuProperties zhipuProperties;
    private final ZhipuAiClient client;

    public ZhiPuEmbedding3Util(ZhipuProperties zhipuProperties) {
        this.zhipuProperties = zhipuProperties;
        this.client = ZhipuAiClient.builder()
                .apiKey(zhipuProperties.getApiKey())
                .build();
    }


    /**
     * 使用智谱API获取文本向量嵌入
     */
    public List<Double> getEmbedding(String text) {
        EmbeddingCreateParams request = EmbeddingCreateParams.builder()
                .model(zhipuProperties.getEmbeddingModel())
                .input(text)
                .dimensions(zhipuProperties.getEmbeddingDimensions())
                .build();

        // 发送请求
        EmbeddingResponse response = client.embeddings().createEmbeddings(request);

        return response.getData().getData().get(0).getEmbedding();
    }

}
