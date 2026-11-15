package campus.reservation.mapper;

import campus.reservation.common.result.PageResult;
import campus.reservation.dto.AdminReservationQueryDTO;
import campus.reservation.entity.Reservation;
import campus.reservation.vo.ReservationVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 管理端分页查询预约记录
     * @param dto
     * @return
     */

    IPage<ReservationVO> selectAdminReservationPage(Page<ReservationVO> page, @Param("dto") AdminReservationQueryDTO dto);
}
