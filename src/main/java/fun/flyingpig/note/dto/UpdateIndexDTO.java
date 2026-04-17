package fun.flyingpig.note.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateIndexDTO {

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
}
