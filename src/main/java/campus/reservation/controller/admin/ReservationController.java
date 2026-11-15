package campus.reservation.controller.admin;

import campus.reservation.common.result.PageResult;
import campus.reservation.common.result.Result;
import campus.reservation.dto.AdminReservationQueryDTO;
import campus.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RequiredArgsConstructor
@RestController("adminReservationController")
@RequestMapping("/api/admin")
@Slf4j
@Tag(name = "管理端预约管理相关接口")
public class ReservationController {

    private final ReservationService reservationService;
    /**
     * 预约记录分页查询
     */
    @GetMapping("/reservations")
    @Operation(summary = "预约记录分页查询")
    public Result<PageResult> page(@ParameterObject AdminReservationQueryDTO queryDTO) {
        log.info("分页查询预约记录 {} " , queryDTO);
        PageResult pageResult = reservationService.page(queryDTO);
        return Result.success(pageResult);
    }
}
