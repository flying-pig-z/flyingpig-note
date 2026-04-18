package fun.flyingpig.note.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.flyingpig.note.service.security.NoteSecurityService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.ServerRequest;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "protocol", havingValue = "STREAMABLE")
public class McpTransportConfig {

    @Bean
    @ConditionalOnMissingBean(WebMvcStreamableServerTransportProvider.class)
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            ObjectProvider<ObjectMapper> objectMapperProvider,
            McpServerStreamableHttpProperties serverProperties
    ) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint(serverProperties.getMcpEndpoint())
                .keepAliveInterval(serverProperties.getKeepAliveInterval())
                .disallowDelete(serverProperties.isDisallowDelete())
                .contextExtractor(this::extractTransportContext)
                .build();
    }

    private McpTransportContext extractTransportContext(ServerRequest request) {
        Map<String, Object> context = new LinkedHashMap<>();
        String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            context.put(NoteSecurityService.MCP_AUTHORIZATION_HEADER_CONTEXT_KEY, authorization);
        }
        return context.isEmpty() ? McpTransportContext.EMPTY : McpTransportContext.create(context);
    }
}
