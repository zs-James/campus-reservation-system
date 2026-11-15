package campus.reservation.controller.user;

import campus.reservation.common.result.Result;
import campus.reservation.dto.PasswordUpdateDTO;
import campus.reservation.dto.UserUpdateDTO;
import campus.reservation.service.UserService;
import campus.reservation.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户管理相关接口")
public class UserController {
    private final UserService userService;

    /**
     * 获取当前登录的用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
    public Result<UserInfoVO> getUserInfo() {
        log.info("获取当前登录用户信息");
        return Result.success(userService.getUserInfo());
    }

    /**
     * 修改个人基本资料
     */
    @PutMapping("/info")
    @Operation(summary = "修改个人基本资料")
    public Result updateUserInfo(@RequestBody UserUpdateDTO userUpdateDTO) {
        log.info("修改个人基本资料");
        userService.updateUserInfo(userUpdateDTO);
        return Result.success();
    }

    /**
     * 修改个人密码
     */
    @Operation(summary = "修改个人密码")
    @PutMapping("/password")
    public Result updatePassword(@RequestBody PasswordUpdateDTO passwordUpdateDTO) {
        log.info("修改个人密码");
        userService.updatePassword(passwordUpdateDTO);
        return Result.success();
    }

}
