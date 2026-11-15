package campus.reservation.controller.user;

import campus.reservation.common.result.Result;
import campus.reservation.dto.LoginDTO;
import campus.reservation.dto.RegisterDTO;
import campus.reservation.service.UserService;
import campus.reservation.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "用户认证相关接口")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        log.info("用户注册: {}", dto);
        userService.register(dto);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        log.info("用户登录: {}", dto);
        return Result.success(userService.login(dto));
    }

   /* @GetMapping("/test")
    public Result<String> test() {
        log.info("测试接口");
        return Result.success("test");
    }*/
}
