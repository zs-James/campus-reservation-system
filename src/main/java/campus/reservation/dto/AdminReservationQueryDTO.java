package campus.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "管理端预约记录查询参数")
public class AdminReservationQueryDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "场地ID")
    private Long venueId;

    @Schema(description = "预约日期，格式：yyyy-MM-dd")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reserveDate;

    @Schema(description = "状态：1已预约 2已取消 3已过期")
    private Integer status;

    @Schema(description = "页码", defaultValue = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer pageSize = 10;
}

