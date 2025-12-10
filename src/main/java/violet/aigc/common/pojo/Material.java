package violet.aigc.common.pojo;

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
    private String coverUrl;
    private String model;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    private Integer status;
    private String extra;

    public violet.aigc.common.proto_gen.aigc.Material toProto() {
        return violet.aigc.common.proto_gen.aigc.Material.newBuilder()
                .setMaterialId(this.materialId)
                .setMaterialType(this.materialType)
                .setUserId(this.userId)
                .setPrompt(this.prompt)
                .setSourceUrl(this.sourceUrl == null ? "" : this.sourceUrl)
                .setMaterialUrl(this.materialUrl)
                .setCoverUrl(this.coverUrl)
                .setModel(this.model)
                .setCreateTime(this.createTime.getTime())
                .setStatus(this.status)
                .setExtra(this.extra == null ? "" : this.extra)
                .build();
    }
}
