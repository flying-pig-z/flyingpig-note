package fun.flyingpig.note.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "zhipu")
public class ZhipuProperties {
    private String apiKey;
    private String embeddingModel;
    private Integer embeddingDimensions;
}