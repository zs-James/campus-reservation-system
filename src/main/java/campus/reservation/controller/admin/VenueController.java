package campus.reservation.controller.admin;

import campus.reservation.common.result.PageResult;
import campus.reservation.common.result.Result;
import campus.reservation.dto.VenueQueryDTO;
import campus.reservation.dto.VenueSaveDTO;
import campus.reservation.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController("adminVenueController")
@RequestMapping("/api/admin/venues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "管理端场地相关接口")
public class VenueController {

    private final VenueService venueService;
    /**
     * 初始化库存
     */
    @Operation(summary = "初始化库存")
    @PostMapping("/stocks/init")
    public Result initStock(
            @RequestParam("venueId") Long id ,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        venueService.initStock(id, date);
        return Result.success();
    }

    /**
     * 新增场地
     */
    @Operation(summary = "新增场地")
    @PostMapping
    public Result addVenue(@RequestBody VenueSaveDTO venueSaveDTO){
        log.info("新增场地 {}" ,venueSaveDTO);
        venueService.addVenue(venueSaveDTO);
        return Result.success();
    }

    /**
     * 修改场地信息
     */
    @Operation(summary = "修改场地信息")
    @PutMapping("/{id}")
    public Result updateVenueMessage(@RequestBody VenueSaveDTO venueSaveDTO , @PathVariable("id") Long id){
        log.info("修改场地信息 {} {}" ,venueSaveDTO , id);
        venueService.updateVenueMessage(venueSaveDTO ,id);
        return Result.success();

    }

    /**
     * 启用/停用场地
     */
    @PutMapping("{id}/status/{status}")
    @Operation(summary = "启用/停用场地")
    public Result startOrStopVenue(@PathVariable("id") Long id ,
                                   @PathVariable("status") Integer status){
        log.info("启用/停用场地: {} {}" , id , status);
        venueService.startOrStopVenue(id , status);
        return Result.success();
    }

    /**
     * 场地分页查询
     */
    @GetMapping
    @Operation(summary = "场地分页查询")
    public Result<PageResult> venuePage(@ParameterObject VenueQueryDTO venueQueryDto){
        log.info("场地分页查询 {}" , venueQueryDto);
        PageResult pageResult = venueService.page(venueQueryDto);

        return Result.success(pageResult);


    }

}
