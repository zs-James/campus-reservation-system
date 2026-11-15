package campus.reservation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueVO {
/*    private String id;  //场地ID
    private String name;  //场地名称
    private String type;//场地类型 0 自习室 1 会议室 3 体育场馆
    private String description;//场地描述
    private String location;//场地位置
    //容量，总库存
    private String capacity;
    private Integer status;//0 表示禁用 1 表示启用
    private LocalDateTime updateTime;//更新时间
    private LocalDateTime createTime;//创建时间
    private LocalDateTime openTime;//开放时间
    private LocalDateTime closeTime;//关闭时间
    private String image;//图片

    private String total;  //实际容量(根据当天实际情况变化的)*/

    private Long id;
    private String name;
    private String type;//场地类型 0 自习室 1 会议室 3 体育场馆
    private String location;//场地位置
    private Integer capacity;//容量，总库存
    private String image;//图片
}
