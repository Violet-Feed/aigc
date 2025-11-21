package violet.aigc.common.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Creation {
    private Long id;
    private Long creationId;
    private Long userId;
    private Long materialId;
    private Integer materialType;
    private String materialUrl;
    private String title;
    private String content;
    private String category;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date modifyTime;
    private Integer status;
    private String extra;
}
