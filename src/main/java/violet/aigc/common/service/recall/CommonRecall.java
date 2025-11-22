package violet.aigc.common.service.recall;

import java.util.Set;

public interface CommonRecall {
    Set<Long> recall(Set<Long> triggerIds);
}
