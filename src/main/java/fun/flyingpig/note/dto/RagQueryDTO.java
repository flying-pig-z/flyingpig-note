package fun.flyingpig.note.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * RAG query request DTO.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagQueryDTO {

    /**
     * Current user question.
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * Selected knowledge base IDs.
     */
    @NotEmpty(message = "至少需要指定一个知识库")
    private List<Long> knowledgeBaseIds;

    /**
     * Recent conversation history kept in memory on the client.
     */
    @Valid
    @Size(max = 10, message = "history最多支持10条消息")
    private List<HistoryMessage> history;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HistoryMessage {

        /**
         * OpenAI-compatible role, for example user or assistant.
         */
        @NotBlank(message = "history.role不能为空")
        private String role;

        /**
         * Message text.
         */
        @NotBlank(message = "history.content不能为空")
        private String content;
    }
}
