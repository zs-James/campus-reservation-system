package campus.reservation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
@Data
public class ReservationCreateDTO {
    @NotNull(message = "场地ID不能为空")
    private Long venueId;

    @NotNull(message = "预约日期不能为空")
    @FutureOrPresent(message = "不能预约过去的日期")
    private LocalDate reserveDate;

    @NotNull(message = "时间段ID不能为空")
    private Long slotId;

    @NotBlank(message = "幂等Token不能为空")
    private String idempotencyKey; // 前端生成的一个唯一UUID，防止重复提交
}
