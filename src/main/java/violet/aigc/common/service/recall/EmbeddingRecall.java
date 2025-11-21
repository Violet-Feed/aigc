package violet.aigc.common.service.recall;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.QueryResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import violet.aigc.common.pojo.RecallResult;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class EmbeddingRecall implements CommonRecall {
    @Override
    public List<RecallResult> recall(List<Long> triggers) {
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .uri("http://localhost:19530")
                .token("root:Milvus")
                .build());

        GetReq getReq = GetReq.builder()
                .collectionName("creation")
                .ids(Arrays.asList(0, 1, 2))
                .outputFields(Arrays.asList("vector", "color"))
                .build();

        GetResp getResp = client.get(getReq);

        List<QueryResp.QueryResult> results = getResp.getGetResults();
        for (QueryResp.QueryResult result : results) {
            System.out.println(result.getEntity());
        }


        return null;
    }
}
