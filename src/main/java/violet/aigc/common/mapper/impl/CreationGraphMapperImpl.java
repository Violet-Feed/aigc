package violet.aigc.common.mapper.impl;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import violet.aigc.common.mapper.CreationGraphMapper;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class CreationGraphMapperImpl implements CreationGraphMapper {
    @Autowired
    private Session session;

    private static final int PAGE_SIZE = 20;

    @Override
    public void createCreation(Long userId, Long creationId) {
        String userVid = String.valueOf(userId);
        String creationVid = "creation:" + creationId;
        String nGQL = String.format(
                "INSERT VERTEX IF NOT EXISTS entity (`entity_type`, `entity_id`) " +
                        "VALUES \"%s\":(\"creation\", %d); " +
                        "INSERT EDGE IF NOT EXISTS author (ts) " +
                        "VALUES \"%s\"->\"%s\":(%d);",
                creationVid, creationId,
                userVid, creationVid, System.currentTimeMillis()
        );
        try {
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("createCreation failed, userId: {}, creationId: {}, error: {}", userId, creationId, resultSet.getErrorMessage());
                throw new RuntimeException("createCreation failed: " + resultSet.getErrorMessage());
            }
        } catch (IOErrorException e) {
            log.error("createCreation failed, userId: {}, creationId: {}", userId, creationId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Long> getCreationIdsByUser(Long userId, Integer page) {
        int offset = (page - 1) * PAGE_SIZE;
        String userVid = String.valueOf(userId);
        String nGQL = String.format(
                "MATCH (u:user)-[r:author]->(e:entity) " +
                        "WHERE id(u) == \"%s\" AND e.entity.entity_type == \"creation\" " +
                        "RETURN " +
                        "e.entity.entity_id AS creation_id, " +
                        "r.ts AS ts " +
                        "ORDER BY ts DESC " +
                        "SKIP %d LIMIT %d",
                userVid, offset, PAGE_SIZE
        );
        try {
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("Query failed - user: {}, error: {}", userId, resultSet.getErrorMessage());
                throw new RuntimeException("Query failed: " + resultSet.getErrorMessage());
            }
            return parseCreations(resultSet);
        } catch (IOErrorException | UnsupportedEncodingException e) {
            log.error("Failed to query creations - user: {}, page: {}", userId, page, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Long> getCreationIdsByFriend(Long userId, Integer page) {
        int offset = (page - 1) * PAGE_SIZE;
        String userVid = String.valueOf(userId);
        String nGQL = String.format(
                "MATCH (u1:user)-[:follow]->(u2:user)-[r:author]->(e:entity) " +
                        "WHERE id(u1) == \"%s\" AND e.entity.entity_type == \"creation\" " +
                        "MATCH (u2)-[:follow]->(u1) " +
                        "RETURN " +
                        "e.entity.entity_id AS creation_id, " +
                        "r.ts AS ts " +
                        "ORDER BY ts DESC " +
                        "SKIP %d LIMIT %d",
                userVid, offset, PAGE_SIZE
        );
        try {
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("Query friend creations failed - user: {}, error: {}", userId, resultSet.getErrorMessage());
                throw new RuntimeException("Query failed: " + resultSet.getErrorMessage());
            }
            return parseCreations(resultSet);
        } catch (IOErrorException | UnsupportedEncodingException e) {
            log.error("Failed to query friend creations - user: {}, page: {}", userId, page, e);
            throw new RuntimeException(e);
        }
    }

    private List<Long> parseCreations(ResultSet resultSet) throws UnsupportedEncodingException {
        List<Long> creationIds = new ArrayList<>();
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            ResultSet.Record record = resultSet.rowValues(i);
            creationIds.add(record.get("creation_id").asLong());
        }
        return creationIds;
    }
}
