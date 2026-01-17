package fun.flyingpig.note.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchImportResult {
    private int importedCount;
    private int failedCount;
    private List<ImportDetail> details;
}