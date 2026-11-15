package campus.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class VenueSaveDTO {
    @NotBlank(message = "场地名称不能为空")
    private String name;

    @NotBlank(message = "场地类型不能为空")
    private String type; // STUDY_ROOM, MEETING_ROOM, SPORTS_VENUE

    private String location;

    @NotNull(message = "容量不能为空")
    private Integer capacity;

    @NotNull(message = "开放时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime openTime;

    @NotNull(message = "关闭时间不能为空")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime closeTime;

    private String description;
    private String image;
}
