package campus.reservation.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisStreamConfig {

    private final StringRedisTemplate stringRedisTemplate;

    public static final String STREAM_KEY = "stream:reservation";
    public static final String GROUP_NAME = "reservation-group";

    //Redis 的 Stream 必须在发消息前先创建好消费者组，否则会报错
    @PostConstruct//在项目启动时检查并创建
    public void initCroup(){
        try {
            // 尝试创建消费者组，从最新的消息开始消费 (0表示从第一条，$表示从最新)
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
        } catch (Exception e) {
            // 如果组已经存在，会抛异常，这里直接忽略
            System.out.println("消费者组已存在，无需重复创建");
        }
    }
}
