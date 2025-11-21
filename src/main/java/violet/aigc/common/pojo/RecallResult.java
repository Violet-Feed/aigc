package violet.aigc.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecallResult {
    private Long triggerId;
    private Long id;
    private Double score;
}
