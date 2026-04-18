package fun.flyingpig.note.controller;

import fun.flyingpig.note.dto.LoginDTO;
import fun.flyingpig.note.dto.LoginResponseDTO;
import fun.flyingpig.note.dto.RegisterDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.User;
import fun.flyingpig.note.service.security.NoteSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final NoteSecurityService noteSecurityService;

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success("登录成功", noteSecurityService.login(loginDTO));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success("登出成功", null);
    }

    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        return Result.success(noteSecurityService.requireCurrentUser());
    }

    @PostMapping("/register")
    public Result<LoginResponseDTO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success("注册成功", noteSecurityService.register(registerDTO));
    }
}
