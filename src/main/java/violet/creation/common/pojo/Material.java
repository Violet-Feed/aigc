package violet.creation.common.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Material {
    private Long id;
    private Long materialId;
    private Integer materialType;
    private Long userId;
    private String prompt;
    private String sourceUrl;
    private String materialUrl;
    private String model;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    private Integer status;
    private String extra;
}
