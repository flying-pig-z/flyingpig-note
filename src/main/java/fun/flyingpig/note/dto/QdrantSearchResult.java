package fun.flyingpig.note.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Qdrant搜索结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QdrantSearchResult {
    private Long id;
    private Double score;
    private Long noteId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private String chunkContent;
}
