package fun.flyingpig.note.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import fun.flyingpig.note.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
