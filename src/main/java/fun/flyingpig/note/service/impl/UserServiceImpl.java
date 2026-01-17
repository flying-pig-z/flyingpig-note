package fun.flyingpig.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.flyingpig.note.dto.LoginDTO;
import fun.flyingpig.note.dto.RegisterDTO;
import fun.flyingpig.note.entity.User;
import fun.flyingpig.note.mapper.UserMapper;
import fun.flyingpig.note.service.UserService;
import fun.flyingpig.note.util.BCryptUtil;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(queryWrapper);
        if (user != null && BCryptUtil.checkPassword(loginDTO.getPassword(), user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return this.getOne(queryWrapper);
    }

    @Override
    public User register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        if (getUserByUsername(registerDTO.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        String hashedPassword = BCryptUtil.hashPassword(registerDTO.getPassword());
        User user = User.builder()
                .username(registerDTO.getUsername())
                .password(hashedPassword)
                .build();
        this.save(user);
        
        return user;
    }
}