package campus.reservation.mapper;

import campus.reservation.entity.TimeSlot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TimeSlotMapper extends BaseMapper<TimeSlot> {

}
