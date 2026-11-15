package campus.reservation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SlotStockVO {
    private Long slotId;
    private String label; // 如 08:00-10:00
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer total;
    private Integer available; // 剩余可用数量
}