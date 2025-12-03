package violet.aigc.common.service.rec;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BeforeRanker {
    private static final int PAGE_SIZE = 20;

    public Set<Long> execute(Set<Long> recallResults, Set<Long> filterIds, Set<Long> backupIds) {
        Set<Long> allRecall = new HashSet<>(recallResults);
        Set<Long> allBackup = new HashSet<>(backupIds);
        allBackup.removeAll(allRecall);

        Set<Long> filteredRecall = new HashSet<>(allRecall);
        filteredRecall.retainAll(filterIds);
        Set<Long> unfilteredRecall = new HashSet<>(allRecall);
        unfilteredRecall.removeAll(filterIds);

        Set<Long> filteredBackup = new HashSet<>(allBackup);
        filteredBackup.retainAll(filterIds);
        Set<Long> unfilteredBackup = new HashSet<>(allBackup);
        unfilteredBackup.removeAll(filterIds);

        Set<Long> result = new HashSet<>(PAGE_SIZE);
        addBucketWithLimit(result, unfilteredRecall);
        addBucketWithLimit(result, unfilteredBackup);
        addBucketWithLimit(result, filteredRecall);
        addBucketWithLimit(result, filteredBackup);
        return result;
    }

    private void addBucketWithLimit(Set<Long> result, Set<Long> bucket) {
        int remaining = PAGE_SIZE - result.size();
        if (remaining <= 0 || bucket.isEmpty()) {
            return;
        }
        bucket.stream()
                .limit(remaining)
                .forEach(result::add);
    }
}
