package fun.flyingpig.note.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Qdrant点数据结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QdrantPoint {
    private Long id;
    private List<Double> vector;
    private Long noteId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private String chunkContent;

}
