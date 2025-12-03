package violet.aigc.common.service.rec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;
import violet.aigc.common.mapper.CreationMapper;
import violet.aigc.common.pojo.Creation;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TrendRecall {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private CreationMapper creationMapper;

    private static final int RECALL_SIZE = 10;

    public Set<Long> recall(Set<Long> triggerIds) {
        List<String> keys = new ArrayList<>();
        if (!triggerIds.isEmpty()) {
            List<Creation> creations = creationMapper.selectByCreationIds(new ArrayList<>(triggerIds));
            List<String> categoryKeys = creations.stream()
                    .map(Creation::getCategory)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(category -> "trend:" + category)
                    .collect(Collectors.toList());
            keys.addAll(categoryKeys);
        }
        keys.add("trend:all");
        List<Set<String>> results = (List<Set<String>>) (List<?>) redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                ZSetOperations<String, String> zSetOps = operations.opsForZSet();
                for (String key : keys) {
                    zSetOps.reverseRange(key, 0, RECALL_SIZE-1);
                }
                return null;
            }
        });

        Set<Long> recallResults = new HashSet<>();
        for (Set<String> result : results) {
            result.stream()
                    .map(Long::valueOf)
                    .forEach(recallResults::add);
        }
        return recallResults;
    }
}
