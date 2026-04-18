package fun.flyingpig.note.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResultDTO {

    private boolean success;
    private String message;
    private String resourceType;
    private Long resourceId;

    public static OperationResultDTO success(String message, String resourceType, Long resourceId) {
        return OperationResultDTO.builder()
                .success(true)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();
    }
}
