package fun.flyingpig.note.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationDTO {

    private boolean valid;
    private Long userId;
    private String username;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expiresAt;

    public static TokenValidationDTO invalid() {
        return TokenValidationDTO.builder()
                .valid(false)
                .build();
    }
}
