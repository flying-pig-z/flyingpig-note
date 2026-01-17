package fun.flyingpig.note.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "知识库标题不能为空")
    @Size(max = 100, message = "知识库标题长度不能超过100")
    private String title;

    @Size(max = 500, message = "知识库描述长度不能超过500")
    private String description;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private Integer noteCount = 0;
    
    /**
     * 知识库索引最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime indexUpdateTime;
}