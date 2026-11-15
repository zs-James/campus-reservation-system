package campus.reservation.controller.user;

import campus.reservation.common.result.PageResult;
import campus.reservation.common.result.Result;
import campus.reservation.dto.LoginDTO;
import campus.reservation.dto.ReservationCreateDTO;
import campus.reservation.dto.ReservationQueryDto;
import campus.reservation.service.ReservationService;
import campus.reservation.vo.ReservationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyVetoException;
import java.net.PortUnreachableException;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户预约相关接口")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 用户预约
     */
    @Operation(summary = "用户预约")
    @PostMapping
    public Result<ReservationVO> makeReservation(@RequestBody ReservationCreateDTO reservationCreateDTO) {
        ReservationVO reservationVO = reservationService.makeReservation(reservationCreateDTO);
        return Result.success(reservationVO);
    }

    /**
     * 用户取消预约
     */
    @Operation(summary = "用户取消预约")
    @PostMapping("/{id}/cancel")
    public Result cancelReservation(@PathVariable("id") Long id) {
        reservationService.cancelReservation(id);
        return Result.success();
    }

    /**
     * 用户查询我的预约(分页)
     */
    @Operation(summary = "我的预约")
    @GetMapping("/my")
    public Result<PageResult> getMyReservations(@ParameterObject ReservationQueryDto reservationQueryDto) {
        PageResult pageResult = reservationService.getMyReservations(reservationQueryDto);
        return Result.success(pageResult);
    }

    /**
     * 预约详情
     */
    @Operation(summary = "预约详情")
    @GetMapping("/{id}")
    public Result<ReservationVO> reservationVOResult(@PathVariable("id") Long id){
        log.info("预约详情，预约id：{}" , id);
        return Result.success(reservationService.reservationVOResult(id));
    }
}
