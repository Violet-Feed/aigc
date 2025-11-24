package violet.aigc.common.config;


import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.uri}")
    private String milvusUri;

    @Value("${milvus.token}")
    private String milvusToken;

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig config = ConnectConfig.builder()
                .uri(milvusUri)
                .token(milvusToken)
                .build();
        return new MilvusClientV2(config);
    }
}
