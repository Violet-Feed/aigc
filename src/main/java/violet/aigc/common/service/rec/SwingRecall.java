package violet.aigc.common.service.rec;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
public class SwingRecall {
    @Autowired
    @Qualifier("swingSession")
    private Session session;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // 用volatile保证多线程可见性（定时任务更新后，recall方法能立即读到最新值）
    private volatile String space = "swing";
    private static final int RECALL_SIZE = 10;

    @PostConstruct
    public void initSpace() {
        refreshSpace();
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshSpaceTask() {
        refreshSpace();
    }

    private void refreshSpace() {
        try {
            String latestSpace = redisTemplate.opsForValue().get("swing-space");
            if (latestSpace != null && !latestSpace.trim().isEmpty()) {
                this.space = latestSpace.trim();
                log.info("成功从kvrocks刷新space，最新值：{}", this.space);
            } else {
                log.warn("kvrocks中未获取到有效space值，保留当前值：{}", this.space);
            }
        } catch (Exception e) {
            log.error("从kvrocks刷新space失败", e);
        }
    }

    public Set<Long> recall(Set<Long> triggerIds) {
        List<String> queries = new ArrayList<>();
        for (Long triggerId : triggerIds) {
            String subQuery = String.format(
                    "(MATCH (source:creation)-[r:sim]->(target:creation) " +
                            "WHERE source.creation.creation_id == '%d' " +
                            "RETURN " +
                            "'%d' AS triggerId, " +
                            "target.creation.creation_id AS targetId, " +
                            "r.simscore AS score " +
                            "ORDER BY score DESC " +
                            "LIMIT %d)",
                    triggerId, triggerId, RECALL_SIZE
            );
            queries.add(subQuery);
        }
        String nGQL = String.join(" UNION ALL ", queries);
        try {
            session.execute("USE " + space);
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("swing recall failed: {}", resultSet.getErrorMessage());
                return Collections.emptySet();
            }
            return parseRecallResult(resultSet);
        } catch (Exception e) {
            log.error("swing recall failed", e);
            return Collections.emptySet();
        }
    }

    private Set<Long> parseRecallResult(ResultSet resultSet) {
        Set<Long> recallResults = new HashSet<>();
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            ResultSet.Record record = resultSet.rowValues(i);
            Long id = record.get("targetId").asLong();
            recallResults.add(id);
        }
        return recallResults;
    }
}
