package campus.reservation.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Venue {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String name;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String type; //场地类型 0 自习室 1 会议室 3 体育场馆
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String location;
    //场地容量，总库存
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private Integer capacity;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String description;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private Integer status;//0 表示禁用 1 表示启用
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private LocalDateTime createTime;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private LocalDateTime updateTime;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private LocalTime openTime;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private LocalTime closeTime;
    @TableField(updateStrategy = FieldStrategy.NOT_EMPTY)
    private String image;
}
