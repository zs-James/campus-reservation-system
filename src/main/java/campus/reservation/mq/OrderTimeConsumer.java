package campus.reservation.mq;

import campus.reservation.common.constant.RedisConstants;
import campus.reservation.entity.Reservation;
import campus.reservation.entity.VenueSlotStock;
import campus.reservation.mapper.ReservationMapper;
import campus.reservation.mapper.VenueSlotStockMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 延迟队列的监听器(订单超时取消消息的消费者)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeConsumer {
    private final RedissonClient redissonClient;
    private final ReservationMapper reservationMapper;
    private final VenueSlotStockMapper venueSlotStockMapper;
    private final StringRedisTemplate stringRedisTemplate;

    //创建一个单线程的线程池
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void startConsume(){
        executorService.submit(()->{
            log.info("Redisson 延迟队列(订单超时监听)启动...");

            //获取阻塞队列，如果没有消息，这里会自动阻塞灯带
            RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(RedisConstants.ORDER_DELAY_QUEUE);

            while (true){
                try {
                    //阻塞等待的消费者用take()拿到订单号（也就是阻塞队列里的数据）
                    String orderNo = (String) blockingDeque.take();
                    log.info("检测到超时订单出队，订单号：{}", orderNo);
                    processOrderTimeOut(orderNo);
                } catch (InterruptedException e) {
                    log.error("订单超时监听任务执行异常", e);
                }
            }
        });
    }

    /**
     * 处理订单超时逻辑
     * @param orderNo
     */
    @Transactional
    private void processOrderTimeOut(String orderNo) {
        //1.根据订单号查询订单
        Reservation reservation = reservationMapper.selectOne(
                new LambdaUpdateWrapper<Reservation>()
                        .eq(Reservation::getOrderNo, orderNo)
        );

        //检查订单是否存在
        if (reservation == null){
            log.info("订单不存在，订单号：{}", orderNo);
            return;
        }

        //2.检查订单状态，如果已经不是1（已预约），说明用户已经取消或支付，无需处理
        if (reservation.getStatus() != 1){
            log.info("订单状态不是1（已预约），无需处理，订单号：{}", orderNo);
            return;
        }

        //3.更新订单状态为3（已过期）
        //采用乐观锁思想，where id = ? and status = 1
        Reservation reservation1 = new Reservation();
        reservation1.setStatus(3);
        reservation1.setActiveFlag(null);//释放唯一索引
        int updateRows = reservationMapper.update(reservation1,
                new LambdaUpdateWrapper<Reservation>()
                        .eq(Reservation::getId, reservation.getId())
                        .eq(Reservation::getStatus, 1));

        // 如果并发下被别人改了状态，直接退出
        if (updateRows == 0){
            return;
        }

        //4.退还mysql库存
        venueSlotStockMapper.update(null,
                new LambdaUpdateWrapper<VenueSlotStock>()
                        .setSql("available = available + 1")
                        .eq(VenueSlotStock::getVenueId, reservation.getVenueId())
                        .eq(VenueSlotStock::getReserveDate, reservation.getReserveDate())
                        .eq(VenueSlotStock::getSlotId, reservation.getSlotId()));

        //5.退还redis库存
        stringRedisTemplate.opsForValue().increment(RedisConstants.STOCK_VENUE_PREFIX + reservation.getVenueId() + ":date:" + reservation.getReserveDate() + ":slot:" + reservation.getSlotId());

        //6.删除场地前端缓存
        stringRedisTemplate.delete(RedisConstants.VENUE_SLOTS_PREFIX + reservation.getVenueId() + ":" +reservation.getReserveDate());

        log.info("订单 {} 超时自动取消处理完成，库存已退还", orderNo);
    }
}
