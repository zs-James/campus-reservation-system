package campus.reservation.task;

import campus.reservation.common.constant.RedisConstants;
import campus.reservation.entity.VenueSlotStock;
import campus.reservation.mapper.VenueSlotStockMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockPreheatTask {
    private final RedissonClient redissonClient;
    private final VenueSlotStockMapper venueSlotStockMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 2 * * ?")
    public void preheatStock() {
        log.info("开始执行库存预热定时任务...");

        //1.尝试获取分布式锁
        RLock lock = redissonClient.getLock(RedisConstants.LOCK_PREHEAT_STOCK);

        //尝试枷锁：最多等待0s（抢不到直接走），只有时间默认30s（看门狗）
        boolean isLock = false;
        try {
            isLock = lock.tryLock(0, -1, TimeUnit.SECONDS);

            if (!isLock){
                log.info("其他节点正在执行库存预热，当前节点跳过");
                return;
            }

            //2.查出来未来三天的库存数据
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(3);

            //查出来未来三天的库存
            List<VenueSlotStock> stockList = venueSlotStockMapper.selectList(
                    new LambdaQueryWrapper<VenueSlotStock>()
                            .ge(VenueSlotStock::getReserveDate, today)
                            .le(VenueSlotStock::getReserveDate, endDate)
            );

            if (stockList == null || stockList.isEmpty()){
                log.info("未来三天没有需要预热的库存数据");
                return;
            }

            //3.批量写入redis
            int count = 0;
            for (VenueSlotStock stock : stockList) {
                // 构造 Key，必须和之前 Lua 脚本扣减用的 Key 保持一致！
                String stockKey = RedisConstants.STOCK_VENUE_PREFIX + stock.getVenueId() +
                        ":date:" + stock.getReserveDate() +
                        ":slot:" + stock.getSlotId();

                //写入redis，设置过期时间为4天，防止旧数据堆积
                stringRedisTemplate.opsForValue().set(
                        stockKey,
                        String.valueOf(stock.getAvailable()),
                        4, TimeUnit.DAYS
                );
                count++;
            }
            log.info("成功预热库存数据，共{}条", count);
        } catch (InterruptedException e) {
            log.error("库存预热任务执行异常", e);
        }finally {
            //4.释放锁，先判断当前线程有没有获取到锁和这个锁是不是当前线程的
            if (lock.isLocked() && lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }
}
