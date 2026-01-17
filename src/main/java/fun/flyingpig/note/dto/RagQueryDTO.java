package fun.flyingpig.note.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * RAG查询请求DTO
 */
@Data
public class RagQueryDTO {

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    private String question;

    /**
     * 指定的知识库ID列表
     */
    @NotEmpty(message = "至少需要指定一个知识库")
    private List<Long> knowledgeBaseIds;

    /**
     * 返回的相关文档数量，默认为5
     */
    private Integer topK = 5;
}
