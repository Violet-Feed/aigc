package violet.aigc.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAction {
    private Long userId;
    private Long creationId;
    private Integer actionType;
    private Long timestamp;
}
