package campus.reservation.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
//@TableName("time_slot")
public class TimeSlot {
    //@TableId(type = IdType.AUTO)
    private Long id;
    private String label;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer sort;
    private Integer status;
}
