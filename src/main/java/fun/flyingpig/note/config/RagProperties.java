package fun.flyingpig.note.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private Long topK;
    private String similarityThreshold;
}
