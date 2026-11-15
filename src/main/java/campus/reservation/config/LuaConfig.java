package campus.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class LuaConfig {

    @Bean
    public DefaultRedisScript<Long> stockDecutScript() {
        // 定义Lua脚本
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        //指定Lua脚本的位置
        script.setLocation(new ClassPathResource("StockDecut.lua"));
        //指定Lua脚本的返回值类型
        script.setResultType(Long.class);
        return script;
    }
}
