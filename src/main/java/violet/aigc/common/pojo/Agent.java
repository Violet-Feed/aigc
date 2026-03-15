package violet.aigc.common.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import violet.aigc.common.proto_gen.aigc.AgentInfo;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agent {
    private Long id;
    private Long agentId;
    private String agentName;
    private String avatarUri;
    private String description;
    private String personality;
    private Long ownerId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date modifyTime;
    private Integer status;
    private String extra;

    public AgentInfo toProto() {
        return AgentInfo.newBuilder()
                .setAgentId(this.agentId)
                .setAgentName(this.agentName)
                .setAvatarUri(this.avatarUri == null ? "" : this.avatarUri)
                .setDescription(this.description == null ? "" : this.description)
                .setPersonality(this.personality)
                .setOwnerId(this.ownerId)
                .setCreateTime(this.createTime.getTime())
                .setModifyTime(this.modifyTime.getTime())
                .setStatus(this.status)
                .setExtra(this.extra == null ? "" : this.extra)
                .build();
    }
}
