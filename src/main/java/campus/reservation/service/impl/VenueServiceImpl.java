package campus.reservation.service.impl;

import campus.reservation.common.constant.RedisConstants;
import campus.reservation.common.result.PageResult;
import campus.reservation.dto.VenueQueryDTO;
import campus.reservation.dto.VenueSaveDTO;
import campus.reservation.entity.TimeSlot;
import campus.reservation.entity.Venue;
import campus.reservation.entity.VenueSlotStock;
import campus.reservation.mapper.*;
import campus.reservation.service.VenueService;
import campus.reservation.vo.SlotStockVO;
import campus.reservation.vo.VenueDetailVO;
import campus.reservation.vo.VenueVO;
import cn.hutool.db.sql.Query;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.core.util.Json;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class VenueServiceImpl implements VenueService {
    @Autowired
    private VenueMapper venueMapper;
    @Autowired
    private TimeSlotMapper timeSlotMapper;
    @Autowired
    private VenueSlotStockMapper venueSlotStockMapper;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 分页查询场地
     */
    @Override
    public PageResult page(VenueQueryDTO venueQueryDTO) {
        /*PageHelper.startPage(venueQueryDTO.getPage(), venueQueryDTO.getPageSize());

        Page<VenueVO> page = venueMapper.page(venueQueryDTO);

        return new PageResult(page.getTotal(), page.getResult());*/

        //1.配置分页条件
        Page<Venue> page = new Page<>(venueQueryDTO.getPage(), venueQueryDTO.getPageSize());
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(venueQueryDTO.getKeyword()), Venue::getName, venueQueryDTO.getKeyword())
                .eq(StringUtils.hasText(venueQueryDTO.getType()) , Venue::getType , venueQueryDTO.getType())
                .eq(venueQueryDTO.getStatus() !=null , Venue::getStatus , venueQueryDTO.getStatus())
                .orderByDesc(Venue::getCreateTime);

        //2.执行分页查询
        venueMapper.selectPage(page , wrapper);

        //3.封装分页数据
        page.convert(venue -> {
            VenueVO venueVO = new VenueVO();
            BeanUtils.copyProperties(venue, venueVO);
            return venueVO;
        });

        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 查询场地详情
     */
    @Override
    public VenueDetailVO venueDetail(Long id) {
        //先去redis中查看是否有该场地的详情
        String venue = stringRedisTemplate.opsForValue().get(RedisConstants.VENUE_DETAIL_PREFIX + id);

        //如果redis中没有，抛异常
        if (venue != null) {
            // 注意：这里如果是防穿透的空值，我们用特殊字符串标记
            if ("_EMPTY_".equals(venue)){
                throw new RuntimeException("场地不存在");
            }
            return JSONUtil.toBean(venue, VenueDetailVO.class);
        }

        // 根据id查询场地详情（查询数据库）
        Venue venueDB = venueMapper.selectById(id);

        //如果数据库中也没有，在redis中存入空值
        if (venueDB == null) {
            // 存入一个标记，过期时间设 2 分钟，防止黑客一直用假 ID 攻击数据库
            stringRedisTemplate.opsForValue().set(RedisConstants.VENUE_DETAIL_PREFIX + id,
                    "_EMPTY_", RedisConstants.EMPTY_TTL, TimeUnit.MINUTES);
            throw new RuntimeException("场地不存在");
        }

        // 组装场地详情VO
        VenueDetailVO venueDetailVO = new VenueDetailVO();
        BeanUtils.copyProperties(venueDB, venueDetailVO);


        //如果数据库中有，存入redis，设置过期时间(随机过期时间，用来防止雪崩)
        stringRedisTemplate.opsForValue().set(RedisConstants.VENUE_DETAIL_PREFIX + id,
                JSONUtil.toJsonStr(venueDetailVO), RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);

        return venueDetailVO;
    }


    /**
     * 查询某天场地的时间槽库存
     */
    @Override
    public List<SlotStockVO> getVenueSlots(Long id, LocalDate date) {
        //先查redis
        String venueDB = stringRedisTemplate.opsForValue().get(RedisConstants.VENUE_SLOTS_PREFIX + id + ":" + date);

        if (venueDB != null) {
            //redis中有，直接返回
            return JSONUtil.toList(venueDB, SlotStockVO.class);
        }

        //redis中没有，再查数据库
        //1.校验场地是否存在
        Venue venue = venueMapper.selectById(id);
        if (venue == null) {
            throw new RuntimeException("场地不存在");
        }

        // 获取场地id
        Long venueId = venue.getId();

        //2.查询出所有有效的时间块模板
        List<TimeSlot> timeSlots = timeSlotMapper.selectList(
                new LambdaQueryWrapper<TimeSlot>()
                        .eq(TimeSlot::getStatus, 1)
                        .orderByAsc(TimeSlot::getSort)
        );

        //3.查询该场地、该日期的所有库存记录
        List<VenueSlotStock> stockList = venueSlotStockMapper.selectList(
                new LambdaQueryWrapper<VenueSlotStock>()
                        .eq(VenueSlotStock::getVenueId, venueId)
                        .eq(VenueSlotStock::getReserveDate, date)
        );

        //4.把库存记录转成map，方便后面根据slotId快速查询,key:slotId , vlaue :Stock(VeneSlotStock对象)
        Map<Long, VenueSlotStock> stockMap = stockList.stream()
                .collect(Collectors.toMap(VenueSlotStock::getSlotId, stock -> stock));

        //5.拼装返回结果
        List<SlotStockVO> result = new ArrayList<>();
        for (TimeSlot timeSlot : timeSlots) {
            //根据时间块模板id，查询库存记录
            VenueSlotStock stock = stockMap.get(timeSlot.getId());

            //如果数据库中有着一条记录，就用数据库中的值
            Integer total = venue.getCapacity();//场地容量
            Integer available = venue.getCapacity();//场地可用容量

            if (stock != null) {
                //如果数据库中有库存，就用数据库中的库存
                total = stock.getTotal();
                available = stock.getAvailable();
            }

            SlotStockVO slotStockVO = SlotStockVO.builder()
                    .slotId(timeSlot.getId())
                    .label(timeSlot.getLabel())
                    .total(total)
                    .available(available)
                    .startTime(timeSlot.getStartTime())
                    .endTime(timeSlot.getEndTime())
                    .build();

            result.add(slotStockVO);
        }

        try {
            //查完数据库后，再写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.VENUE_SLOTS_PREFIX + id + ":" + date,
                    JSONUtil.toJsonStr(result), RedisConstants.SLOT_NULL_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.out.println("写入缓存失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 查询热门场地
     */
    @Override
    public List<VenueVO> getHotVenues() {
        // 1. 从 Redis ZSet 中获取分数最高的前 10 个场地 ID (降序)
        Set<String> topVenueIds = stringRedisTemplate.opsForZSet().reverseRange(RedisConstants.HOT_RANK_KEY, 0, 9);

        if (topVenueIds == null || topVenueIds.isEmpty()){
            return Collections.emptyList();//如果没有数据，返回空集合
        }

        // 2. 把 Set 转换成 List<Long>
        List<Long> venueIds = topVenueIds.stream().map(Long::valueOf).collect(Collectors.toList());

        // 3. 根据 ID 去数据库查出详细信息（注意：这里用 in 查询，不要用 for 循环查库）
        List<Venue> venueList = venueMapper.selectBatchIds(venueIds);

        // 4. 为了保证返回的顺序和 ZSet 排行榜的顺序一致，我们需要对查出来的数据重新排序
        // 因为 selectBatchIds 查出来的顺序可能是乱的
        Map<Long, Venue> venueMap = venueList.stream().collect(Collectors.toMap(Venue::getId, venue -> venue));

        //5.遍历ids，封装结果
        ArrayList<VenueVO> resultList = new ArrayList<>();
        for (Long venueId : venueIds) {
            Venue venue = venueMap.get(venueId);
            if (venue != null){
                VenueVO venueVO = VenueVO.builder()
                        .id(venue.getId())
                        .name(venue.getName())
                        .image(venue.getImage())
                        .capacity(venue.getCapacity())
                        .type(venue.getType())
                        .location(venue.getLocation())
                        .build();

                resultList.add(venueVO);
            }
        }


        return resultList;
    }

    /**
     * 查询热门场地（测试用）
     *//*
    @Override
    public List<VenueVO> getHotVenuesTest() {
        //现在还没有预约记录，暂时用venue的容量返回
        //1.获取所有场地
        List<Venue> venueList = venueMapper.selectList(
                new LambdaQueryWrapper<Venue>()
                        .eq(Venue::getStatus, 1)
                        .orderByDesc(Venue::getCapacity)
                        .last("limit 5")
        );


        //2.返回结果
        List<VenueVO> venueTop5 = venueList.stream().map(venue -> {
            VenueVO vo = new VenueVO();
            vo.setId(venue.getId());
            vo.setName(venue.getName());
            vo.setType(venue.getType());
            vo.setLocation(venue.getLocation());
            vo.setCapacity(venue.getCapacity());
            vo.setImage(venue.getImage());
            return vo;
        }).collect(Collectors.toList());

        return venueTop5;
    }*/

    /**
     * 初始化场地库存
     */
    @Override
    @Transactional
    public void initStock(Long venueId, LocalDate date) {
        //1.根据场地id查询场地
        Venue venue = venueMapper.selectById(venueId);

        //2.判断场地是否存在
        if (venue == null) {
            throw new RuntimeException("场地不存在");
        }

        //3.查询该场地当天是否生成过库存
        Long count = venueSlotStockMapper.selectCount(
                new LambdaQueryWrapper<VenueSlotStock>()
                        .eq(VenueSlotStock::getVenueId, venueId)
                        .eq(VenueSlotStock::getReserveDate, date)
        );

        if (count > 0) {
            throw new RuntimeException("该场地当天已生成过库存");
        }

        //4.查出所有有效的时间块模板
        List<TimeSlot> timeSlots = timeSlotMapper.selectList(
                new LambdaQueryWrapper<TimeSlot>()
                        .eq(TimeSlot::getStatus, 1)
        );

        //5.遍历时间块，给每个时间段在库存表中插入一条记录，total 和 available 都等于场地的capacity
        for (TimeSlot timeSlot : timeSlots) {
            VenueSlotStock venueSlotStock = VenueSlotStock.builder()
                    .venueId(venueId)
                    .slotId(timeSlot.getId())
                    .reserveDate(date)
                    .total(venue.getCapacity())
                    .available(venue.getCapacity())
                    .build();

            //6.如果是会议室，total设为1
            if (venue.getType().equals("1")) {
                venueSlotStock.setTotal(1);
                venueSlotStock.setAvailable(1);
            }

            venueSlotStockMapper.insert(venueSlotStock);
        }
    }

    /**
     * 新增场地
     * @param venueSaveDTO
     */
    @Override
    public void addVenue(VenueSaveDTO venueSaveDTO) {
        Venue venue = new Venue();
        if (venueSaveDTO.getCapacity() <= 0){
            throw new RuntimeException("容量不能为零");
        }
        BeanUtils.copyProperties(venueSaveDTO ,venue);
        venue.setStatus(1);//新增的场地默认状态为1
        venueMapper.insert(venue);
    }

    /**
     * 修改场地信息
     * @param venueSaveDTO
     * @param venueId
     */
    @Override
    public void updateVenueMessage(VenueSaveDTO venueSaveDTO, Long venueId) {
        //根据场地id查询场地信息
        Venue venueDB = venueMapper.selectById(venueId);

        //判断场地是否存在
        if (venueDB == null){
            throw new RuntimeException("场地不存在");
        }

        //修改场地信息
        Venue venue = new Venue();
        BeanUtils.copyProperties(venueSaveDTO , venue);
        /*venueMapper.update(venue ,
                new LambdaUpdateWrapper<Venue>()
                        .eq(Venue::getId , venueId)
        );*/
        venue.setId(venueId);
        venueMapper.updateById(venue);

        //删除redis中的场地详情
        stringRedisTemplate.delete(RedisConstants.VENUE_DETAIL_PREFIX + venueId);
    }

    /**
     * 启用/停用场地
     * @param venueId
     * @param status
     */
    @Override
    public void startOrStopVenue(Long venueId, Integer status) {
        //根据场地id查询场地信息
        Venue venueDB = venueMapper.selectById(venueId);

        //判断该场地是否存在
        if (venueDB == null){
            throw new RuntimeException("该场地不存在");
        }

        //把场地的status设为0 停用
        Venue venue = new Venue();
        venue.setStatus(0);

        //修改场地状态
        /*venueMapper.update(venue,
                new LambdaUpdateWrapper<Venue>()
                        .eq(Venue::getId , venueId)
        );*/
        venueMapper.updateById(venue);

        //删除redis中的场地详情缓存
        stringRedisTemplate.delete(RedisConstants.VENUE_DETAIL_PREFIX + venueId);
    }



}
