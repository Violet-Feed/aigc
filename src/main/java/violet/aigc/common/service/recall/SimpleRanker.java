package violet.aigc.common.service.recall;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SimpleRanker {
    public List<Long> rank(Set<Long> recallResults, Set<Long> filterIds) {
        List<Long> filteredIds = recallResults.stream()
                .filter(id -> !filterIds.contains(id))
                .collect(Collectors.toList());
        Collections.shuffle(filteredIds);
        int limit = Math.min(20, filteredIds.size());
        return filteredIds.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
