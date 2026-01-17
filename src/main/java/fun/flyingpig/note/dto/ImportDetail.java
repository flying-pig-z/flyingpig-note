package fun.flyingpig.note.dto;

import lombok.Data;

@Data
public class ImportDetail {
    private String fileName;
    private String status; // 成功、失败、跳过
    private String message;
}