package fun.flyingpig.note.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoteDTO {

    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 200, message = "笔记标题长度不能超过200")
    private String title;

    private String content;

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
}
