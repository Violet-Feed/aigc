package violet.aigc.common.service.recall;

import org.springframework.stereotype.Component;
import violet.aigc.common.pojo.RecallResult;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SimpleRanker {
    public List<Long> rank(List<RecallResult> recallResults, List<Long> filterIds) {
        return recallResults.stream().map(RecallResult::getId).collect(Collectors.toList());
    }
}
