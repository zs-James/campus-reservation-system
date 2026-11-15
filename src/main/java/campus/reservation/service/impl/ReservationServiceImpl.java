package campus.reservation.service.impl;

import campus.reservation.common.constant.RedisConstants;
import campus.reservation.common.result.PageResult;
import campus.reservation.common.result.Result;
import campus.reservation.common.utils.OrderNoUtil;
import campus.reservation.common.utils.UserContext;
import campus.reservation.config.LuaConfig;
import campus.reservation.config.RedisStreamConfig;
import campus.reservation.dto.AdminReservationQueryDTO;
import campus.reservation.dto.ReservationCreateDTO;
import campus.reservation.dto.ReservationQueryDto;
import campus.reservation.entity.Reservation;
import campus.reservation.entity.TimeSlot;
import campus.reservation.entity.Venue;
import campus.reservation.entity.VenueSlotStock;
import campus.reservation.mapper.ReservationMapper;
import campus.reservation.mapper.TimeSlotMapper;
import campus.reservation.mapper.VenueMapper;
import campus.reservation.mapper.VenueSlotStockMapper;
import campus.reservation.service.ReservationService;
import campus.reservation.vo.ReservationVO;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.sun.jdi.IntegerValue;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.actuate.autoconfigure.metrics.PropertiesAutoTimer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class ReservationServiceImpl implements ReservationService {
    private final ReservationMapper reservationMapper;
    private final VenueSlotStockMapper venueSlotStockMapper;
    private final VenueMapper venueMapper;
    private final TimeSlotMapper timeSlotMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> stockDecutScript;
    private final RedissonClient redissonClient;

    /**
     * 用户预约
     *
     * @param reservationCreateDTO the reservation create dto
     */
    @Override
    @Transactional
    public ReservationVO makeReservation(ReservationCreateDTO reservationCreateDTO) {
        //获取用户id
        Long userId = UserContext.getUserId();

        //查询idempotency_key是否存在
        Long idemCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getIdempotencyKey, reservationCreateDTO.getIdempotencyKey())
        );

        //判断idempotency_key是否存在(防止前端狂点)
        if (idemCount > 0) {
            throw new RuntimeException("请勿重复提交订单");
        }


        /*//扣除库存
        int updateRows = venueSlotStockMapper.update(
                new LambdaUpdateWrapper<VenueSlotStock>()
                        .setSql("available = available - 1")
                        .eq(VenueSlotStock::getVenueId, reservationCreateDTO.getVenueId())
                        .eq(VenueSlotStock::getReserveDate, reservationCreateDTO.getReserveDate())
                        .eq(VenueSlotStock::getSlotId, reservationCreateDTO.getSlotId())
                        .gt(VenueSlotStock::getAvailable, 0)
        );

        if (updateRows == 0) {
            throw new RuntimeException("库存不足");
        }*/

        //构造 Redis 的库存 Key（和查缓存、预热时的 Key 必须保持一致）
        String stockKey = RedisConstants.STOCK_VENUE_PREFIX + reservationCreateDTO.getVenueId() + ":date:" + reservationCreateDTO.getReserveDate() + ":slot:" + reservationCreateDTO.getSlotId();

        //使用lua脚本判断库存是否充足并扣减库存
        Long result = stringRedisTemplate.execute(
                stockDecutScript,
                Collections.singletonList(stockKey),
                "1"
        );

        int value = result.intValue();
        //判断结果是否为1（是否成功扣减）
        if (value != 1) {
            //如果结果不为1，也就是没有成功扣减库存，根据返回结果抛异常
            throw new RuntimeException(value == -1 ? "库存尚未初始化，请稍后再试": value == 0 ? "手慢了，该时段已经被抢光了" : "系统繁忙，请稍后再试");
        }

        //生成订单
        Reservation reservation = Reservation.builder()
                .orderNo(OrderNoUtil.generateOrderNo())
                .userId(userId)
                .reserveDate(reservationCreateDTO.getReserveDate())
                .status(1)//1 表示已预约
                .idempotencyKey(reservationCreateDTO.getIdempotencyKey())
                .venueId(reservationCreateDTO.getVenueId())
                .activeFlag(1)//1 表示生效
                .slotId(reservationCreateDTO.getSlotId())
                .build();

        //设置订单过期时间，假设15分钟未签到/支付则自动取消
        reservation.setExpireTime(LocalDateTime.now().plusMinutes(15));

        /*//新增订单
        try {
            // 这里的插入会触发数据库的唯一索引 uk_user_active
            // 如果用户已经约过这个时段，会抛出 DuplicateKeyException
            reservationMapper.insert(reservation);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 捕获唯一索引冲突，抛出友好提示，如果新增订单失败，需要把redis中的库存加回来
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("您已经预约过该场地的这个时间段了，请勿重复预约");
        }*/

        //发送消息到MQ
        try {
            //创建map集合，用来匹配stream中的键值对结构
            Map<String,String> messageMap = new HashMap<>();
            messageMap.put("reservationData" , JSONUtil.toJsonStr(reservation));

            //发送消息到Redis Stream
            stringRedisTemplate.opsForStream().add(
                    RedisStreamConfig.STREAM_KEY , messageMap
            );
        } catch (Exception e) {
            //发送失败，回滚，把redis中的库存加回来
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new RuntimeException("系统异常，预约失败");
        }

        //将订单号放入延迟队列，延迟15分钟
        //获取阻塞队列
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(RedisConstants.ORDER_DELAY_QUEUE);
        //创建延迟队列，将阻塞队列包装为延迟队列
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);

        // 把订单号扔进去，设置 15 分钟后过期。注意这里单位是分钟。
        // 要确保 LocalDateTime 的 15 分钟和这里的 15 分钟保持一致
        delayedQueue.offer(reservation.getOrderNo() , 15 , TimeUnit.MINUTES);


        //删除缓存中的数据
        stringRedisTemplate.delete(RedisConstants.VENUE_SLOTS_PREFIX + reservationCreateDTO.getVenueId() + ":" + reservationCreateDTO.getReserveDate());

        //将对应redis的有序集合中的score值加一，用于排行榜
        stringRedisTemplate.opsForZSet().incrementScore(RedisConstants.HOT_RANK_KEY , reservationCreateDTO.getVenueId().toString(), 1);

        //封装reservationVo并返回
        Venue venue = venueMapper.selectById(reservationCreateDTO.getVenueId());
        TimeSlot timeSlot = timeSlotMapper.selectById(reservationCreateDTO.getSlotId());

        ReservationVO vo = new ReservationVO();
        vo.setId(reservation.getId());
        vo.setOrderNo(reservation.getOrderNo());
        vo.setVenueId(venue.getId());
        vo.setVenueName(venue.getName());
        vo.setVenueLocation(venue.getLocation());
        vo.setReservationDate(reservation.getReserveDate());
        vo.setSlotLabel(timeSlot.getLabel());
        vo.setStatus(reservation.getStatus());
        vo.setStatusDesc("已预约");
        vo.setCreateTime(reservation.getCreateTime() != null ? reservation.getCreateTime() : LocalDateTime.now());


        return vo;
    }

    /**
     * 用户取消预约
     */
    @Override
    @Transactional
    public void cancelReservation(Long reservationId) {
        //获取当前用户id
        Long userId = UserContext.getUserId();

        //1.查询这个预约单子是否存在
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约单子不存在");
        }

        //2.校验这个单子是不是当前用户的
        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("无权限取消其他用户的预约");
        }

        //3.校验单子的状态，只有“已预约”的单子才能取消
        if (reservation.getStatus() != 1) {
            throw new RuntimeException("只有" + "已预约" + "的单子才能取消");
        }

        //4.取消预约，即修改订单状态为已取消 2  active_flag null ,cancel_reason
        int updateRows = reservationMapper.update(reservation,
                new LambdaUpdateWrapper<Reservation>()
                        .set(Reservation::getStatus, 2)
                        .set(Reservation::getActiveFlag, null)
                        .set(Reservation::getCancelReason, "用户取消")
                        .eq(Reservation::getId, reservationId)
                        .eq(Reservation::getStatus, 1)// 只有状态为已预约的订单才能取消
        );

        if (updateRows <= 0) {
            throw new RuntimeException("取消预约失败");
        }
        //5.把可用库存 available = available + 1

        int updateRow = venueSlotStockMapper.update(new LambdaUpdateWrapper<VenueSlotStock>()
                .setSql("available = available + 1")
                .eq(VenueSlotStock::getVenueId, reservation.getVenueId())
                .eq(VenueSlotStock::getReserveDate, reservation.getReserveDate())
                .eq(VenueSlotStock::getSlotId, reservation.getSlotId()));

        if (updateRow <= 0) {
            throw new RuntimeException("回滚库存失败，请联系管理员");
        }

        //6.回滚redis中的库存
        // 注意：StockKey 必须和秒杀扣减时的 Key 完全一致！
        String stockKey = RedisConstants.STOCK_VENUE_PREFIX + reservation.getVenueId() + ":date:" + reservation.getReserveDate() + ":slot:" + reservation.getSlotId();
        stringRedisTemplate.opsForValue().increment(stockKey);

        //6.删除前端查询缓存
        stringRedisTemplate.delete(RedisConstants.VENUE_SLOTS_PREFIX + reservation.getVenueId() + ":" + reservation.getId());

        //7.将redis中对应场地的有序数组的score之减一
        stringRedisTemplate.opsForZSet().incrementScore(RedisConstants.HOT_RANK_KEY , reservation.getVenueId().toString() , -1);
    }

    /**
     * 获取我的预约
     */
    @Override
    public PageResult getMyReservations(ReservationQueryDto reservationQueryDto) {
       /* //获取当前用户id
        Long userId = UserContext.getUserId();

        PageHelper.startPage(reservationQueryDto.getPage(), reservationQueryDto.getPageSize());

        //查询数据,需要关联查出场地名称，时间段标签
        List<ReservationVO> reservations = reservationMapper.selectMyReservations(userId, reservationQueryDto.getStatus());

        reservations.forEach(reservationVO -> {
            reservationVO.setStatusDesc(reservationVO.getStatus() == 1 ? "已预约" : reservationVO.getStatus() == 2 ? "已取消" : "已过期");
        });

        //Page<ReservationVO> voPage = (Page<ReservationVO>) reservations;
        //return new PageResult(voPage.getTotal() , voPage.getResult());

        PageInfo<ReservationVO> pageInfo = new PageInfo<>(reservations);

        return new PageResult(pageInfo.getTotal(), pageInfo.getList());*/

        //1.构建查询条件
        Page<Reservation> page = new Page<>(reservationQueryDto.getPage(), reservationQueryDto.getPageSize());
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(reservationQueryDto.getStatus() != null , Reservation::getStatus , reservationQueryDto.getStatus());

        //2.执行分页查询
        reservationMapper.selectPage(page, wrapper);

        //3.封装结果并返回
        page.convert(reservation -> {
            ReservationVO reservationVO = new ReservationVO();
            BeanUtils.copyProperties(reservation, reservationVO);
            reservationVO.setStatusDesc(reservationVO.getStatus() == 1 ? "已预约" : reservationVO.getStatus() == 2 ? "已取消" : "已过期");
            return reservationVO;
        });

        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 预约详情
     */
    @Override
    public ReservationVO reservationVOResult(Long id) {
        //根据预约id查询预约信息
        Reservation reservation = reservationMapper.selectById(id);

        //再根据venueId查询场地信息
        Venue venue = venueMapper.selectById(reservation.getVenueId());

        //再根据slotId查询时间段信息
        TimeSlot timeSlot = timeSlotMapper.selectById(reservation.getSlotId());

        //创建一个reservationVo对象，往里面封装数据
        ReservationVO reservationVO = ReservationVO.builder()
                .id(reservation.getVenueId())
                .reservationDate(reservation.getReserveDate())
                .orderNo(reservation.getOrderNo())
                .status(reservation.getStatus())
                .venueId(reservation.getVenueId())
                .venueLocation(venue.getLocation())
                .venueName(venue.getName())
                .createTime(reservation.getCreateTime())
                .startTime(timeSlot.getStartTime().toString())
                .endTime(timeSlot.getEndTime().toString())
                .slotLabel(timeSlot.getLabel())
                .statusDesc(reservation.getStatus() == 1 ? "已预约" : reservation.getStatus() == 2 ? "已取消" : "已过期")
                .build();
        return reservationVO;
    }

    /**
     * 分页查询预约记录
     */
    @Override
    public PageResult page(AdminReservationQueryDTO queryDTO) {
        Page<ReservationVO> page = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        reservationMapper.selectAdminReservationPage(page, queryDTO);

        return new PageResult(page.getTotal(), page.getRecords());
    }
}
