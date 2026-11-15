package campus.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "场地分页查询参数")
public class VenueQueryDTO {
    @Schema(description = "搜索关键字" ,example = "自习室")
    private String keyword;

    @Schema(description = "场地类型" ,example = "0")
    private String type;

    @Schema(description = "页码" ,example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数" ,example = "10")
    private Integer pageSize = 10;

    @Schema(description = "状态" ,example = "1")
    private Integer status;
}
