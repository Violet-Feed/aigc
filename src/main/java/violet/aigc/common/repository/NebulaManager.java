package violet.aigc.common.repository;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NebulaManager {

    private final NebulaPool nebulaPool;
    private final String username;
    private final String password;
    private static final String DEFAULT_SPACE = "violet";

    public NebulaManager(NebulaPool nebulaPool,
                         @Value("${nebula.username}") String username,
                         @Value("${nebula.password}") String password) {
        this.nebulaPool = nebulaPool;
        this.username = username;
        this.password = password;
    }

    public ResultSet execute(String nGQL) {
        Session session = null;
        try {
            session = nebulaPool.getSession(username, password, false);
            ResultSet useResult = session.execute("USE " + DEFAULT_SPACE + ";");
            if (!useResult.isSucceeded()) {
                String msg = "USE " + DEFAULT_SPACE + " failed: " + useResult.getErrorMessage();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            ResultSet resultSet = session.execute(nGQL);
            return resultSet;
        } catch (IOErrorException e) {
            log.error("Nebula execute failed, nGQL: {}", nGQL, e);
            throw new RuntimeException("Nebula execute failed", e);
        } catch (Exception e) {
            log.error("Nebula execute unexpected error, nGQL: {}", nGQL, e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                try {
                    session.release();
                } catch (Exception ignore) {
                    log.debug("Nebula session release failed (ignored)", ignore);
                }
            }
        }
    }
}

