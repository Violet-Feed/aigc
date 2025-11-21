package violet.aigc.common.service.recall;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilterRetrieval {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public List<Long> retrieve(Long userId) {
        return null;
    }
}
