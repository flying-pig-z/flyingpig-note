package fun.flyingpig.note.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轻量索引状态DTO，只保留判断增量更新所需字段。
 */
@Data
public class NoteIndexLatestUpdateDTO {

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 该笔记最近一次索引更新时间
     */
    private LocalDateTime latestUpdateTime;
}
