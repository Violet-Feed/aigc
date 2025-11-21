package violet.aigc.common.service.recall;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import violet.aigc.common.pojo.RecallResult;

import java.util.List;

@Slf4j
@Component
public class HotRecall implements CommonRecall {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public List<RecallResult> recall(List<Long> triggers) {
        return null;
    }
}
