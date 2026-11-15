package campus.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationQueryDto {
    private Integer status;// 可选：筛选状态
    private Integer page = 1;
    private Integer pageSize = 10;
}
