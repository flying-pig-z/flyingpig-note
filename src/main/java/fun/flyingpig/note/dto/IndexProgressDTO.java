package fun.flyingpig.note.dto;

import lombok.Data;

@Data
public class IndexProgressDTO {

    private Long knowledgeBaseId;

    private boolean forceUpdate;

    private String stage;

    private String message;

    private Integer totalNotes;

    private Integer processedNotes;

    private Integer insertedCount;

    private Integer updatedCount;

    private Integer skippedCount;

    private Integer deletedCount;

    private Integer progressPercent;

    private Long currentNoteId;

    private String currentNoteTitle;

    private String currentAction;
}
