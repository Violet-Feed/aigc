package violet.aigc.common.repository;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class NebulaSwingManager {

    private final NebulaPool nebulaPool;
    private final RedisTemplate<String, String> redisTemplate;
    private final String username;
    private final String password;
    private static final String SWING_SPACE_KEY = "swing-space";
    private static final String DEFAULT_SPACE = "swing";

    private volatile String currentSpace = DEFAULT_SPACE;

    public NebulaSwingManager(NebulaPool nebulaPool,
                              RedisTemplate<String, String> redisTemplate,
                              @Value("${nebula.username}") String username,
                              @Value("${nebula.password}") String password) {
        this.nebulaPool = nebulaPool;
        this.redisTemplate = redisTemplate;
        this.username = username;
        this.password = password;
    }

    @PostConstruct
    public void initSpace() {
        refreshSpaceInternal(true);
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshSpaceTask() {
        refreshSpaceInternal(false);
    }

    private void refreshSpaceInternal(boolean logOnInit) {
        try {
            String latestSpace = redisTemplate.opsForValue().get(SWING_SPACE_KEY);
            if (latestSpace != null && !latestSpace.trim().isEmpty()) {
                String trimmed = latestSpace.trim();
                this.currentSpace = trimmed;
                log.info("NebulaSwingManager 刷新 swing space 成功，最新值：{}", this.currentSpace);
            } else {
                if (logOnInit) {
                    log.warn("NebulaSwingManager 初始化时未从 kvrocks 获取到有效 space，使用默认值：{}",
                            this.currentSpace);
                } else {
                    log.warn("NebulaSwingManager 定时刷新时未获取到有效 space，沿用当前值：{}",
                            this.currentSpace);
                }
            }
        } catch (Exception e) {
            log.error("NebulaSwingManager 从 kvrocks 刷新 space 失败，沿用当前值：{}", this.currentSpace, e);
        }
    }

    public ResultSet execute(String nGQL) {
        String space = this.currentSpace;
        return executeWithSpace(space, nGQL);
    }

    public ResultSet executeWithSpace(String space, String nGQL) {
        Session session = null;
        try {
            session = nebulaPool.getSession(username, password, false);

            ResultSet useResult = session.execute("USE " + space + ";");
            if (!useResult.isSucceeded()) {
                String msg = "USE " + space + " failed: " + useResult.getErrorMessage();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            ResultSet resultSet = session.execute(nGQL);
            return resultSet;
        } catch (IOErrorException e) {
            log.error("NebulaSwingManager execute failed, space: {}, nGQL: {}", space, nGQL, e);
            throw new RuntimeException("NebulaSwingManager execute failed", e);
        } catch (Exception e) {
            log.error("NebulaSwingManager execute unexpected error, space: {}, nGQL: {}", space, nGQL, e);
            throw new RuntimeException(e);
        } finally {
            if (session != null) {
                try {
                    session.release();
                } catch (Exception ex) {
                    log.debug("Nebula swing session release failed (ignored)", ex);
                }
            }
        }
    }
}

