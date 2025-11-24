package violet.aigc.common.mapper.impl;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import violet.aigc.common.mapper.CreationGraphMapper;
import violet.aigc.common.pojo.Creation;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class CreationGraphMapperImpl implements CreationGraphMapper {
    @Autowired
    private Session session;

    private String space = "violet";

    private static final int PAGE_SIZE = 20;

    @Override
    public List<Creation> getCreationsByUser(Long userId, Integer page) {
        int offset = (page - 1) * PAGE_SIZE;
        String nGQL = String.format(
                "USE %s;" +
                        "MATCH (u:user)-[r:create]->(c:creation) " +
                        "WHERE u.user_id == '%d' " +
                        "RETURN " +
                        "c.creation_id AS creation_id, " +
                        "c.title AS title, " +
                        "c.cover_url AS cover_url, " +
                        "c.user_id AS user_id" +
                        "r.timestamp AS timestamp " +
                        "ORDER BY r.timestamp DESC " +
                        "SKIP %d LIMIT %d",
                space, userId, offset, PAGE_SIZE
        );
        try {
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("Query failed - space: {}, user: {}, error: {}", space, userId, resultSet.getErrorMessage());
                throw new RuntimeException("Query failed: " + resultSet.getErrorMessage());
            }
            return parseCreations(resultSet);
        } catch (IOErrorException | UnsupportedEncodingException e) {
            log.error("Failed to query creations - space: {}, user: {}, page: {}", space, userId, page, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Creation> getCreationsByDigg(Long userId, Integer page) {
        int offset = (page - 1) * PAGE_SIZE;
        String nGQL = String.format(
                "USE %s;" +
                        "MATCH (u:user)-[r:digg]->(c:creation) " +
                        "WHERE u.user_id == '%d' " +
                        "RETURN " +
                        "c.creation_id AS creation_id, " +
                        "c.title AS title, " +
                        "c.cover_url AS cover_url, " +
                        "c.user_id AS user_id" +
                        "r.timestamp AS timestamp " +
                        "ORDER BY r.timestamp DESC " +
                        "SKIP %d LIMIT %d",
                space, userId, offset, PAGE_SIZE
        );
        try {
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("Query digg failed - space: {}, user: {}, error: {}", space, userId, resultSet.getErrorMessage());
                throw new RuntimeException("Query failed: " + resultSet.getErrorMessage());
            }
            return parseCreations(resultSet);
        } catch (IOErrorException | UnsupportedEncodingException e) {
            log.error("Failed to query digg creations - space: {}, user: {}, page: {}", space, userId, page, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Creation> getCreationsByFriend(Long userId, Integer page) {
        int offset = (page - 1) * PAGE_SIZE;
        String nGQL = String.format(
                "USE %s;" +
                        "MATCH (u1:user)-[f1:follow]->(u2:user)-[r:create]->(c:creation) " +
                        "WHERE u1.user_id == '%d' " +
                        "MATCH (u2)-[f2:follow]->(u1) " +
                        "RETURN " +
                        "c.creation_id AS creation_id, " +
                        "c.title AS title, " +
                        "c.cover_url AS cover_url, " +
                        "c.user_id AS user_id" +
                        "r.timestamp AS timestamp, " +
                        "ORDER BY r.timestamp DESC " +
                        "SKIP %d LIMIT %d",
                space, userId, offset, PAGE_SIZE
        );
        try {
            ResultSet resultSet = session.execute(nGQL);
            if (!resultSet.isSucceeded()) {
                log.error("Query friend creations failed - space: {}, user: {}, error: {}", space, userId, resultSet.getErrorMessage());
                throw new RuntimeException("Query failed: " + resultSet.getErrorMessage());
            }
            return parseCreations(resultSet);
        } catch (IOErrorException | UnsupportedEncodingException e) {
            log.error("Failed to query friend creations - space: {}, user: {}, page: {}", space, userId, page, e);
            throw new RuntimeException(e);
        }
    }

    private List<Creation> parseCreations(ResultSet resultSet) throws UnsupportedEncodingException {
        List<Creation> creations = new ArrayList<>();
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            ResultSet.Record record = resultSet.rowValues(i);
            Creation creation = new Creation();
            creation.setCreationId(record.get("creation_id").asLong());
            creation.setTitle(record.get("title").asString());
            creation.setMaterialUrl(record.get("cover_url").asString());
            creation.setUserId(record.get("user_id").asLong());
            creations.add(creation);
        }
        return creations;
    }
}
