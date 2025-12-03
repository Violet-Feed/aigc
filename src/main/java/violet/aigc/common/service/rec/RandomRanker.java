package violet.aigc.common.service.rec;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RandomRanker {
    private static final int PAGE_SIZE = 20;

    public List<Long> rank(Set<Long> recallResults) {
        List<Long> recallResultList = new ArrayList<>(recallResults);
        Collections.shuffle(recallResultList);
        return recallResultList.stream()
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());
    }
}
