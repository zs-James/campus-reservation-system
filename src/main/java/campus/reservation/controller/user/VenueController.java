package campus.reservation.controller.user;

import campus.reservation.common.result.PageResult;
import campus.reservation.common.result.Result;
import campus.reservation.dto.VenueQueryDTO;
import campus.reservation.service.VenueService;
import campus.reservation.vo.SlotStockVO;
import campus.reservation.vo.VenueDetailVO;
import campus.reservation.vo.VenueVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
@Tag(name = "场地管理相关接口")
public class VenueController {

    private final VenueService venueService;

    /**
     * 场地分页查询
     */
    @Operation(summary = "分页查询场地")
    @GetMapping
    public Result<PageResult> page(@ParameterObject VenueQueryDTO venueQueryDTO) {
        log.info("分页查询场地，页码：{}，每页大小：{}", venueQueryDTO.getPage(), venueQueryDTO.getPageSize());
        venueQueryDTO.setStatus(1); // 【强制覆盖】：不管你前端传啥，我统统改为 1（只查开放）
        PageResult pageResult = venueService.page(venueQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取热门场地（top10）
     */
    @Operation(summary = "获取热门场地（top10）")
    @GetMapping("/hot")
    public Result<List<VenueVO>> getHotVenues() {
        log.info("获取热门场地（top10）");
        List<VenueVO> venueVOList = venueService.getHotVenues();
        return Result.success(venueVOList);
    }

    /**
     * 获取热门场地（top5）,现在没有预约记录，先用场地容量代替
     *//*
    @Operation(summary = "获取热门场地（top5测试版）")
    @GetMapping("/hot/test")
    public Result<List<VenueVO>> getHotVenuesTest() {
        log.info("获取热门场地（top5测试版）");
        List<VenueVO> venueVOList = venueService.getHotVenuesTest();
        return Result.success(venueVOList);
    }*/

    /**
     * 场地详情
     */
    @Operation(summary = "查询场地详情")
    @GetMapping("/{id}")
    public Result<VenueDetailVO> venueDetail(@PathVariable Long id) {
        log.info("查询场地详情，场地ID：{}", id);
        VenueDetailVO venueDetailVO = venueService.venueDetail(id);
        return Result.success(venueDetailVO);
    }

    @Operation(summary = "查询某天场地的时间槽库存")
    @GetMapping("/{id}/slots")
    public Result<List<SlotStockVO>> getVenueSlots(
            @Parameter(description = "场地ID") @PathVariable("id") Long id,
            @Parameter(description = "预约日期，格式：yyyy-MM-dd")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<SlotStockVO> result = venueService.getVenueSlots(id, date);
        return Result.success(result);
    }


}
