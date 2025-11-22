package violet.aigc.common.service.recall;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class SwingRecall implements CommonRecall {
    @Autowired
    private Session session;

    private String space;

    private static final int RECALL_SIZE = 100;

    @Override
    public Set<Long> recall(Set<Long> triggerIds) {
        List<String> queries = new ArrayList<>();
        for (Long triggerId : triggerIds) {
            String subQuery = String.format(
                    "(MATCH (source:item)-[r:similar]->(target:item) " +
                            "WHERE id(source) == '%d' " +
                            "RETURN " +
                            "'%d' AS triggerId, " +
                            "id(target) AS id, " +
                            "r.score AS score " +
                            "ORDER BY r.score DESC " +
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
            Long id = record.get("id").asLong();
            recallResults.add(id);
        }
        return recallResults;
    }
}
