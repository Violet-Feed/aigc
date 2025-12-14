package violet.aigc.common.config;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

@Configuration
public class NebulaConfig {

    @Value("${nebula.address}")
    private String address;

    @Value("${nebula.port}")
    private int port;

    @Value("${nebula.username}")
    private String username;

    @Value("${nebula.password}")
    private String password;

    @Bean
    public NebulaPool nebulaPool() {
        NebulaPool pool = new NebulaPool();
        NebulaPoolConfig config = new NebulaPoolConfig();
        config.setMaxConnSize(100);
        config.setMinConnSize(10);
        config.setIdleTime(180000);
        config.setIntervalIdle(60000);
        List<HostAddress> addresses = Collections.singletonList(new HostAddress(address, port));
        try {
            pool.init(addresses, config);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return pool;
    }
}
