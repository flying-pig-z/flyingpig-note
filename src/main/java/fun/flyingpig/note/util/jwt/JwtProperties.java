package fun.flyingpig.note.util.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "mySecretKey123456789012345678901234567890";
    private Long expiration = 86400000L; // 24小时
    private String header = "Authorization";
    private String prefix = "Bearer ";
}