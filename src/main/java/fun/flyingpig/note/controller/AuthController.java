package fun.flyingpig.note.controller;


import fun.flyingpig.note.dto.LoginDTO;
import fun.flyingpig.note.dto.LoginResponseDTO;
import fun.flyingpig.note.dto.RegisterDTO;
import fun.flyingpig.note.dto.Result;
import fun.flyingpig.note.entity.User;
import fun.flyingpig.note.service.UserService;
import fun.flyingpig.note.util.jwt.JwtUtil;
import fun.flyingpig.note.util.jwt.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO);
        if (user != null) {
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            LoginResponseDTO responseDTO = new LoginResponseDTO(token, user);
            return Result.success("登录成功", responseDTO);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 用户登出 (JWT无状态，客户端删除token即可)
     */
    @PostMapping("/logout")
    public Result logout() {
        return Result.success("登出成功");
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            return Result.success(user);
        } else {
            return Result.error(401, "未登录");
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<LoginResponseDTO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        User user = userService.register(registerDTO);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponseDTO responseDTO = new LoginResponseDTO(token, user);
        return Result.success("注册成功", responseDTO);
    }
}