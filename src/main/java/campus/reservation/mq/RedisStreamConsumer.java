package campus.reservation.mq;

import campus.reservation.common.constant.RedisConstants;
import campus.reservation.config.RedisStreamConfig;
import campus.reservation.entity.Reservation;
import campus.reservation.entity.VenueSlotStock;
import campus.reservation.mapper.ReservationMapper;
import campus.reservation.mapper.VenueSlotStockMapper;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 编写消费者，监听Redis Stream消息
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisStreamConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final VenueSlotStockMapper venueSlotStockMapper;
    private final ReservationMapper reservationMapper;


    //创建一个单线程池去后台一直监听
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void startConsume(){
        executorService.submit(()->{
            log.info("开始监听Redis Stream消息");

            while (true){
                try {
                    //1.读取消息 xreadgroup group consumername count 1 block 2 streams key >
                    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                            Consumer.from(RedisStreamConfig.GROUP_NAME, "consumer-1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(RedisStreamConfig.STREAM_KEY, ReadOffset.lastConsumed())
                    );

                    //判断队列中是否有订单
                    if (records != null && !records.isEmpty()){
                        //队列中有订单，解析订单
                        //遍历订单,防御性编程，以防后面一次从队列中获取的订单数据变多
                        for (MapRecord<String, Object, Object> record : records) {
                            processMessage(record);
                        }
                    }
                } catch (Exception e) {
                    log.error("读取stream消息异常", e);
                }
            }
        });
    }

    //处理消息(解析消息)
    private void processMessage(MapRecord<String, Object, Object> record) {
        try {
            //2.获取消息体
            Map<Object, Object> value = record.getValue();

            //2.1.获取消息体中的预约对象
            String jsonStr =(String) value.get("reservationData");

            //2.2.将json字符串转换为Reservation对象
            Reservation reservation = JSONUtil.toBean(jsonStr, Reservation.class);

            //3.扣减mysql中的库存
            int updateRows = venueSlotStockMapper.update(
                    null,
                    new LambdaUpdateWrapper<VenueSlotStock>()
                            .setSql("available = available - 1")
                            .eq(VenueSlotStock::getVenueId, reservation.getVenueId())
                            .eq(VenueSlotStock::getReserveDate, reservation.getReserveDate())
                            .eq(VenueSlotStock::getSlotId, reservation.getSlotId())
            );

            //判断库存是否扣减成功
            if (updateRows == 0){
                //库存扣减失败
                log.error("数据库库存不足，订单号：{}", reservation.getOrderNo());
                //redis库存回滚
                stringRedisTemplate.opsForValue().increment(RedisConstants.STOCK_VENUE_PREFIX + reservation.getVenueId() + ":date:" + reservation.getReserveDate() + ":slot:" + reservation.getSlotId());
            }else {
                //库存扣减成功，新增预约订单
                reservationMapper.insert(reservation);
                log.info("新增预约订单成功，订单号：{}", reservation.getOrderNo());
            }

            //4.确认消息（ACK）xack key group consumer id
            stringRedisTemplate.opsForStream().acknowledge(RedisStreamConfig.STREAM_KEY, RedisStreamConfig.GROUP_NAME, record.getId());
        } catch (Exception e) {
            log.error("处理消息失败，消息id：{}", record.getId(), e);

            /*//判断重试次数
            //获取这条消息的投递次数(delivery count)
            PendingMessagesSummary summary = stringRedisTemplate.opsForStream().pending(
                    RedisStreamConfig.STREAM_KEY,
                    RedisStreamConfig.GROUP_NAME
            );
            //单独查出这条消息的投递次数（这里用伪代码说明逻辑，实际可查单条pending）
            long deliveryCount = getDeliveryCount(record.getId().toString());

            if (deliveryCount >= 3){
                log.error("消息 {} 重试超过 3 次，转入死信队列（DLQ）", record.getId());
                //直接ack，防止消息重复处理
                stringRedisTemplate.opsForStream().acknowledge(RedisStreamConfig.STREAM_KEY, RedisStreamConfig.GROUP_NAME, record.getId());
            }*/

            //如果不足 3 次，就不 ACK，留给定时任务去 Pending List 里重新捞它
        }
    }

    /**
     * 获取消息的投递次数
     * @param messageId 消息id
     * @return 消息的投递次数
     */
    private long getDeliveryCount(String messageId) {
        try {
            //查询指定Id的pending消息
            PendingMessages pendingMessages = stringRedisTemplate.opsForStream().pending(
                    RedisStreamConfig.STREAM_KEY,
                    RedisStreamConfig.GROUP_NAME,
                    Range.just(messageId),
                    1L
            );

            if (pendingMessages != null && !pendingMessages.isEmpty()){
                return pendingMessages.get(0).getTotalDeliveryCount();
            }
        } catch (NumberFormatException e) {
            log.error("获取消息投递次数失败",e);
        }
        return 0;
    }

    /**
     * 定时处理 Pending List (死信重试)
     * 每分钟执行一次
     */
    @Scheduled(fixedDelay = 60000)
    public void  processPendingMessage(){
        try {
            //1.查询 pending-list里的消息
            // 参数说明：Consumer Group, Stream Key, 查询范围（- 到 + 表示所有）, 每次拉取的数量
            PendingMessages pendingMessages = stringRedisTemplate.opsForStream().pending(
                    RedisStreamConfig.GROUP_NAME,
                    RedisStreamConfig.STREAM_KEY,
                    Range.unbounded(),// 查询所有消息(- 到 +)
                    10L// 每次拉取10条消息
            );

            //判断是否有积压消息 pendingMessages == null 永远为false，因为如果队列里没有积压消息，它返回的是一个空的 PendingMessages 对象（里面的 size() 为 0），而不是 null
            if (pendingMessages.isEmpty()){
                //如果没有积压消息，直接返回
                return;
            }

            log.info("发现 {} 条未确认的 Pending 消息，准备重试处理", pendingMessages.size());

            //2.遍历积压消息，重新入队
            for (PendingMessage pendingMessage : pendingMessages) {
                //获取重试次数
                long deliveryCount = pendingMessage.getTotalDeliveryCount();

                //如果重试次数超过3次，转入死信队列（已记录日志代替）
                if (deliveryCount >= 3){
                    log.error("消息 {} 重试超过 3 次，转入死信队列（DLQ）", pendingMessage.getId());

                    //强制ack
                    stringRedisTemplate.opsForStream().acknowledge(
                            RedisStreamConfig.STREAM_KEY,
                            RedisStreamConfig.GROUP_NAME,
                            pendingMessage.getId()
                    );
                }
                //没有超过3次，判断闲置时间是否超过60s
                //2.1.检查消息的闲置时间（elapsedTime），如果超过了60秒还没ACK，大概率是异常了
                if (pendingMessage.getElapsedTimeSinceLastDelivery().getSeconds() > 60){
                    //3.把消息Claim（认领）过来，意思是不管这条消息之前是谁处理的，现在交给我（consumer-1）处理
                    List<MapRecord<String, Object, Object>> claimedRecords = stringRedisTemplate.opsForStream().claim(
                            RedisStreamConfig.STREAM_KEY,
                            RedisStreamConfig.GROUP_NAME,
                            "consumer-1",// 消费者名称
                            Duration.ofSeconds(60),// 安全阈值，只有限制时间超过此值的消息才会被认领
                            pendingMessage.getId()//要认领的消息id
                    );

                    //4.重新执行处理逻辑claimedRecords != null永远为true，原因跟上面那个一样
                    if (!claimedRecords.isEmpty()) {
                        for (MapRecord<String, Object, Object> record : claimedRecords) {
                            processMessage(record);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理 Pending 消息异常", e);
        }
    }
}
