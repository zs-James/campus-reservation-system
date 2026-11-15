package campus.reservation.mapper;

import campus.reservation.dto.VenueQueryDTO;
import campus.reservation.entity.Venue;
import campus.reservation.vo.VenueVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VenueMapper extends BaseMapper<Venue> {

}
