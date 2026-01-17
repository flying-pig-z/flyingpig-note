package fun.flyingpig.note.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Qdrant向量数据库配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantProperties {

    /**
     * Qdrant服务地址
     */
    private String host = "http://8.210.250.29:6333";

    /**
     * Collection名称
     */
    private String collectionName = "note_vectors";

    /**
     * 向量维度（需要与embedding模型输出维度一致）
     */
    private Integer vectorSize = 1024;

    /**
     * 相似度计算方式: Cosine, Euclid, Dot
     */
    private String distance = "Cosine";

    /**
     * API Key（如果Qdrant启用了认证）
     */
    private String apiKey;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 30000;
}