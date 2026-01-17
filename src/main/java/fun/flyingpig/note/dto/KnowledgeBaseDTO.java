package fun.flyingpig.note.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeBaseDTO {

    @NotBlank(message = "知识库标题不能为空")
    @Size(max = 100, message = "知识库标题长度不能超过100")
    private String title;

    @Size(max = 500, message = "知识库描述长度不能超过500")
    private String description;
    
    /**
     * 知识库索引最后更新时间
     */
    private LocalDateTime indexUpdateTime;
}