package fun.flyingpig.note.util.embedding;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.embedding.EmbeddingCreateParams;
import ai.z.openapi.service.embedding.EmbeddingResponse;
import fun.flyingpig.note.config.ZhipuProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZhiPuEmbedding3Util {

    @Autowired
    private ZhipuProperties zhipuProperties;


    /**
     * 使用智谱API获取文本向量嵌入
     */
    public List<Double> getEmbedding(String text) {
        ZhipuAiClient client = ZhipuAiClient.builder().apiKey(zhipuProperties.getApiKey()).build();

        EmbeddingCreateParams request = EmbeddingCreateParams.builder()
                .model(zhipuProperties.getEmbeddingModel())
                .input(text)
                .dimensions(zhipuProperties.getEmbeddingDimensions())
                .build();

        // 发送请求
        EmbeddingResponse response = client.embeddings().createEmbeddings(request);

        System.out.println("向量: " + response.getData().getData().get(0).getEmbedding());
        return response.getData().getData().get(0).getEmbedding();
    }

}
