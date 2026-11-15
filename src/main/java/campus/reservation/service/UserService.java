package campus.reservation.service;

import campus.reservation.dto.LoginDTO;
import campus.reservation.dto.PasswordUpdateDTO;
import campus.reservation.dto.RegisterDTO;
import campus.reservation.dto.UserUpdateDTO;
import campus.reservation.vo.LoginVO;
import campus.reservation.vo.UserInfoVO;
import jakarta.validation.Valid;

public interface UserService {
    /**
     * 注册用户
     * @param dto
     */
    void register(@Valid RegisterDTO dto);

    /**
     * 用户登录
     * @param dto
     * @return
     */
    LoginVO login(@Valid LoginDTO dto);

    /**
     * 获取当前登录的用户信息
     * @return
     */
    UserInfoVO getUserInfo();

    /**
     * 更新用户信息
     * @param userUpdateDTO
     */
    void updateUserInfo(UserUpdateDTO userUpdateDTO);

    /**
     * 更新用户密码
     * @param passwordUpdateDTO
     */
    void updatePassword(PasswordUpdateDTO passwordUpdateDTO);
}
