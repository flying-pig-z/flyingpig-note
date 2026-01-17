package fun.flyingpig.note.dto;

import lombok.Data;

import java.util.List;

/**
 * RAG回答响应DTO
 */
@Data
public class RagAnswerDTO {

    /**
     * AI生成的回答
     */
    private String answer;

    /**
     * 参考的相关文档
     */
    private List<RelevantDocument> relevantDocuments;

    @Data
    public static class RelevantDocument {
        /**
         * 笔记ID
         */
        private Long noteId;

        /**
         * 笔记标题
         */
        private String noteTitle;

        /**
         * 相关内容片段
         */
        private String content;

        /**
         * 相似度得分
         */
        private Double score;
    }
}
