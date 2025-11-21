package violet.aigc.common.service.recall;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import violet.aigc.common.pojo.UserAction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TriggerRetrieval {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final DateTimeFormatter YYYYMMDD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");


    public List<Long> retrieve(Long userId) {
        List<String> dateList = getLast7DaysList();
        List<String> redisKeys = dateList.stream()
                .map(date -> "action:" + userId + ":" + date)
                .collect(Collectors.toList());
        List<String> jsonList = redisTemplate.opsForValue().multiGet(redisKeys);
        List<String> processedJsonList = jsonList.stream()
                .filter(Objects::nonNull)
                .map(json -> json.replaceAll(",\\s*$", ""))
                .collect(Collectors.toList());
        String combinedJson = "[" + String.join(",", processedJsonList) + "]";
        List<UserAction> actions = JSON.parseArray(combinedJson, UserAction.class);
        return actions.stream().map(UserAction::getCreationId).collect(Collectors.toList());
    }

    private List<String> getLast7DaysList() {
        List<String> dateList = new ArrayList<>(7);
        LocalDate currentDate = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = currentDate.minusDays(i);
            String formattedDate = targetDate.format(YYYYMMDD_FORMATTER);
            dateList.add(formattedDate);
        }
        return dateList;
    }

}
