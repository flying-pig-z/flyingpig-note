package fun.flyingpig.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("note")
public class Note extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 200, message = "笔记标题长度不能超过200")
    private String title;

    private String content;

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
}
