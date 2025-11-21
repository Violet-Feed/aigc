package violet.aigc.common.service.recall;

import violet.aigc.common.pojo.RecallResult;

import java.util.List;

public interface CommonRecall {
    List<RecallResult> recall(List<Long> triggers);
}
