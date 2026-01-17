package fun.flyingpig.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import fun.flyingpig.note.dto.LoginDTO;
import fun.flyingpig.note.dto.RegisterDTO;
import fun.flyingpig.note.entity.User;

public interface UserService extends IService<User> {

    /**
     * 用户登录
     */
    User login(LoginDTO loginDTO);

    /**
     * 用户注册
     */
    User register(RegisterDTO registerDTO);

    /**
     * 根据用户名查找用户
     */
    User getUserByUsername(String username);
}

