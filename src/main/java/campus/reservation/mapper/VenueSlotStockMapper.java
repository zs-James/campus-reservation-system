package campus.reservation.mapper;

import campus.reservation.entity.VenueSlotStock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface VenueSlotStockMapper extends BaseMapper<VenueSlotStock> {

}
