package violet.aigc.common.service.rec;

import com.vesoft.nebula.client.graph.data.ResultSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import violet.aigc.common.repository.NebulaSwingManager;

import java.util.*;

@Slf4j
@Component
public class SwingRecall {
    @Autowired
    private NebulaSwingManager nebulaSwingManager;
    private static final int RECALL_SIZE = 10;

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
            ResultSet resultSet = nebulaSwingManager.execute(nGQL);
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
