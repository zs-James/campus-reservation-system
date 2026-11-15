package campus.reservation.service;

import campus.reservation.common.result.PageResult;
import campus.reservation.dto.AdminReservationQueryDTO;
import campus.reservation.dto.ReservationCreateDTO;
import campus.reservation.dto.ReservationQueryDto;
import campus.reservation.vo.ReservationVO;
import org.yaml.snakeyaml.events.Event;

public interface ReservationService {
    /**
     * 用户预约
     * @param reservationCreateDTO the reservation create dto
     */
    ReservationVO makeReservation(ReservationCreateDTO reservationCreateDTO);

    /**
     * 用户取消预约
     */
    void cancelReservation(Long reservationId);

    /**
     * 用户查询我的预约(分页)
     */
    PageResult getMyReservations(ReservationQueryDto reservationQueryDto);

    /**
     * 查看预约详情
     * @param id
     * @return
     */
    ReservationVO reservationVOResult(Long id);

    /**
     * 管理端预约记录分页查询
     */
    PageResult page(AdminReservationQueryDTO queryDTO);
}
