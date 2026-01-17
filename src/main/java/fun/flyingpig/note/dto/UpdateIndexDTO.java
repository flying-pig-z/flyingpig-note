package fun.flyingpig.note.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 更新索引请求DTO
 */
@Data
public class UpdateIndexDTO {

    /**
     * 知识库ID
     */
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
}
