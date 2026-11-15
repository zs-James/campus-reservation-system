package campus.reservation.interceptor;

import campus.reservation.common.utils.JwtUtil;
import campus.reservation.common.utils.UserContext;
import campus.reservation.entity.User;
import campus.reservation.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jboss.marshalling.util.ShortFieldPutter;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的 Token
        String token = request.getHeader("Authorization");

        // 2. 如果没带 Token 或为空，直接拦截
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(401);
            return false;
        }

        // 3. 兼容前端带 "Bearer " 前缀的情况
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        //System.out.println("Token: " + token);
        // 4. 校验 Token 是否有效
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401); // Token 过期或伪造
            return false;
        }

        // 5. 解析出 userId 存入 ThreadLocal
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            UserContext.setUserId(userId);

            //如果是 /api/admin/ 开头的请求，校验是不是管理员
            String requestURI = request.getRequestURI();
            if (requestURI.startsWith("/api/admin/")){
                User user = userMapper.selectById(userId);
                if (user == null || user.getRole() != 1 ){
                    response.setStatus(403);// 403 Forbidden 代表权限不足
                }
            }


            return true; // 放行
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

        /*String token = request.getHeader("Authorization");

        if (token == null || token.trim().isEmpty()) {
            System.out.println("拦截：Token 为空");
            response.setStatus(401);
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            // 校验 Token，如果解析失败会直接跳到 catch 块
            Long userId = jwtUtil.getUserIdFromToken(token);
            UserContext.setUserId(userId);
            System.out.println("拦截器放行，当前用户ID: " + userId);
            return true; // 放行
        } catch (Exception e) {
            // 【关键】：把具体的异常打印出来
            System.out.println("Token 解析失败，具体原因: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(401);
            return false;
        }
    }*/

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 6. 请求结束，清理 ThreadLocal 防止内存泄漏
        UserContext.remove();
    }
}
