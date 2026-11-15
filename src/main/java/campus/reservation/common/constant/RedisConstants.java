package campus.reservation.common.constant;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import io.netty.util.internal.ThreadLocalRandom;


public class RedisConstants {

    public static final String VENUE_DETAIL_PREFIX = "venue:detail:";
    //防雪崩：过期时间 = 30分钟 + 0~5分钟随机值
    public static final Long CACHE_NULL_TTL = 30L + ThreadLocalRandom.current().nextLong(6);
    public static final String  VENUE_SLOTS_PREFIX = "venue:slots:";
    public static final Long SLOT_NULL_TTL = 10L;
    public static final String RESERVATION_PREFIX = "reservation:";
    //放穿透
    public static final Long EMPTY_TTL = 2L;
    public static final String STOCK_VENUE_PREFIX = "stock:venue:";
    //分布式锁(用于预热库存)
    public static final String LOCK_PREHEAT_STOCK = "lock:task:preheat_stock";
    public static final String HOT_RANK_KEY = "rank:venue:hot";
    // 订单超时延迟队列的 Key
    public static final String ORDER_DELAY_QUEUE = "queue:order:delay";
}
