package fun.flyingpig.note.dto;

import fun.flyingpig.note.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {

    private String token;
    private String tokenType = "Bearer";
    private User userInfo;

    // 原有构造函数保持不变
    public LoginResponseDTO(String token, User userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }
}