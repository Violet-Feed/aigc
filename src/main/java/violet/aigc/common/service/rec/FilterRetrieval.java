package violet.aigc.common.service.rec;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;
import violet.aigc.common.utils.TimeUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FilterRetrieval {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Set<Long> retrieve(Long userId) {
        List<String> dateList = TimeUtil.getLast7DaysList();
        List<String> keys = dateList.stream()
                .map(date -> "expo:" + userId + ":" + date)
                .collect(Collectors.toList());
        List<List<String>> results = (List<List<String>>) (List<?>) redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                for (String key : keys) {
                    operations.opsForList().range(key, 0, -1);
                }
                return null;
            }
        });
        Set<Long> filterIds = new HashSet<>();
        for (List<String> result : results) {
            if (result == null) continue;
            for (String str : result) {
                filterIds.addAll(Arrays.stream(str.split(",")).map(Long::valueOf).collect(Collectors.toList()));
            }
        }
        return filterIds;
    }
}
