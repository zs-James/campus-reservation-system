package campus.reservation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationVO {
    private Long id;
    private String orderNo;
    private Long venueId;
    private String venueName;
    private String venueLocation;
    private LocalDate reservationDate;
    private String slotLabel;
    private String startTime;
    private String endTime;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
    //用户端并不需要，管理端需要
    private String userName;
}
