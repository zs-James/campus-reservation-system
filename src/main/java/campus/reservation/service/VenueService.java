package campus.reservation.service;

import campus.reservation.common.result.PageResult;
import campus.reservation.dto.VenueQueryDTO;
import campus.reservation.dto.VenueSaveDTO;
import campus.reservation.vo.SlotStockVO;
import campus.reservation.vo.VenueDetailVO;
import campus.reservation.vo.VenueVO;

import java.time.LocalDate;
import java.util.List;

public interface VenueService {
    /**
     * 分页查询场地
     */
    PageResult page(VenueQueryDTO venueQueryDTO);

    /**
     * 查询场地详情
     */
    VenueDetailVO venueDetail(Long id);


    /**
     * 查询某天场地的时间槽库存
     */
    List<SlotStockVO> getVenueSlots(Long id, LocalDate date);

    /**
     * 查询热门场地
     */
    List<VenueVO> getHotVenues();

    /**
     * 查询热门场地（测试用）
     *//*
    List<VenueVO> getHotVenuesTest();*/

    /**
     * 初始化库存
     * @param id
     * @param date
     */
    void initStock(Long id, LocalDate date);

    /**
     * 新增场地
     * @param venueSaveDTO
     */
    void addVenue(VenueSaveDTO venueSaveDTO);

    /**
     * 修改场地信息
     * @param venueSaveDTO
     * @param id
     */
    void updateVenueMessage(VenueSaveDTO venueSaveDTO, Long id);

    /**
     * 启用/停用场地
     * @param id
     * @param status
     */
    void startOrStopVenue(Long id, Integer status);


}
