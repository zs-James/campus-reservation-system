package campus.reservation.service.impl;

import campus.reservation.common.utils.JwtUtil;
import campus.reservation.common.utils.UserContext;
import campus.reservation.dto.LoginDTO;
import campus.reservation.dto.PasswordUpdateDTO;
import campus.reservation.dto.RegisterDTO;
import campus.reservation.dto.UserUpdateDTO;
import campus.reservation.entity.User;
import campus.reservation.mapper.UserMapper;
import campus.reservation.service.UserService;
import campus.reservation.vo.LoginVO;
import campus.reservation.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
//@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注册用户
     * @param dto
     */
    @Override
    public void register(RegisterDTO dto) {
        //检查用户名是否存在
        User user = new User();
        BeanUtils.copyProperties(dto,user);

        User userDB = userMapper.selectByUsername(user.getUsername());
        if(userDB != null){
            // 用户已存在
            throw new RuntimeException("用户名已存在");
        }

        //用户不存在，往数据库中添加数据
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setRole(0);// 默认角色为普通用户
        user.setStatus(1);// 默认状态为正常
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));// 对用户密码进行加密

        // 添加用户数据到数据库
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {

        //查询用户信息
        User userDB = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        //不存在提示用户先注册
        if (userDB == null) {
            throw new RuntimeException("账号或密码错误");
        }
        //存在--判断用户帐号状态是否正常
        if (userDB.getStatus() == 0) {
            throw new RuntimeException("用户帐号状态异常，请联系管理员");
        }

        //检查密码是否正确
        String loginPassword = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());// 对传进来的用户密码进行加密
        if(!loginPassword.equals(userDB.getPassword())){
            throw new RuntimeException("账号或密码错误");
        }

        // 4. 生成 Token (这里只传 user.getId())
        String token = jwtUtil.generateToken(userDB.getId());

        // 5. 返回 VO
        return LoginVO.builder()
                .token(token)
                .userId(userDB.getId())
                .nickname(userDB.getNickname())
                .role(userDB.getRole())
                .build();
    }

    /**
     * 获取当前登录的用户信息
     * @return
     */
    @Override
    public UserInfoVO getUserInfo() {
        //获取用户id
        Long userId = UserContext.getUserId();

        //根据userId查询用户信息
        User user = userMapper.selectById(userId);

        //封装并返回Vo对象
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    /**
     * 更新用户信息
     * @param userUpdateDTO
     */
    @Override
    public void updateUserInfo(UserUpdateDTO userUpdateDTO) {
        //获取userId
        Long userId = UserContext.getUserId();
       //new一个user空对象，把dto中的值赋进去
        User user = new User();

        //把dto中的值赋给user对象
        BeanUtils.copyProperties(userUpdateDTO, user);
        user.setId(userId);

        //更新数据库中用户信息
        userMapper.updateById(user);

    }

    /**
     * 更新用户密码
     * @param passwordUpdateDTO
     */
    @Override
    public void updatePassword(PasswordUpdateDTO passwordUpdateDTO) {
        //获取用户id
        Long userId = UserContext.getUserId();

        User user = new User();

        user.setPassword(DigestUtils.md5DigestAsHex(passwordUpdateDTO.getNewPassword().getBytes()));
        user.setId(userId);

        userMapper.updateById(user);
    }


}
