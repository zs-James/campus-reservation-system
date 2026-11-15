package campus.reservation.vo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueDetailVO extends VenueVO {
    private LocalDate openTime;
    private LocalDate closeTime;
    private String description;
    private Integer status;
}
