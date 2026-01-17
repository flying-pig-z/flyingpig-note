package fun.flyingpig.note.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新索引结果DTO
 */
@Data
public class UpdateIndexResultDTO {

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 新增索引的笔记数量
     */
    private Integer insertedCount;

    /**
     * 更新索引的笔记数量
     */
    private Integer updatedCount;

    /**
     * 跳过的笔记数量(索引已是最新)
     */
    private Integer skippedCount;
    
    /**
     * 删除的索引数量(对应笔记已被删除)
     */
    private Integer deletedCount;

    /**
     * 处理详情
     */
    private List<NoteIndexDetail> details;

    @Data
    public static class NoteIndexDetail {
        /**
         * 笔记ID
         */
        private Long noteId;

        /**
         * 笔记标题
         */
        private String noteTitle;

        /**
         * 操作类型: INSERT, UPDATE, SKIP
         */
        private String action;

        /**
         * 处理消息
         */
        private String message;
    }
}