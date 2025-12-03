package violet.aigc.common.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Creation {
    private Long id;
    private Long creationId;
    private Long userId;
    private String coverUrl;
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

    public violet.aigc.common.proto_gen.aigc.Creation toProto() {
        return violet.aigc.common.proto_gen.aigc.Creation.newBuilder()
                .setCreationId(this.creationId)
                .setUserId(this.userId)
                .setCoverUrl(this.coverUrl == null ? "" : this.coverUrl)
                .setMaterialId(this.materialId)
                .setMaterialType(this.materialType)
                .setMaterialUrl(this.materialUrl == null ? "" : this.materialUrl)
                .setTitle(this.title)
                .setContent(this.content == null ? "" : this.content)
                .setCategory(this.category == null ? "" : this.category)
                .setCreateTime(this.createTime.getTime())
                .setModifyTime(this.modifyTime.getTime())
                .setStatus(this.status)
                .setExtra(this.extra == null ? "" : this.extra)
                .build();
    }
}
