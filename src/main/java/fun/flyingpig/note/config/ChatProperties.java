package fun.flyingpig.note.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
}
