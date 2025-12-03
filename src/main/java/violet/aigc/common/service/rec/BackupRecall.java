package violet.aigc.common.service.rec;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class BackupRecall {
    @Autowired
    private MilvusClientV2 milvusClient;

    private static final int RECALL_SIZE = 100;

    public Set<Long> recall() {
        FloatVec queryEmbedding = new FloatVec(generateRandomVector());
        SearchReq searchReq = SearchReq.builder()
                .collectionName("creation")
                .data(Collections.singletonList(queryEmbedding))
                .annsField("rec_embeddings")
                .topK(RECALL_SIZE)
                .build();
        List<List<SearchResp.SearchResult>> searchResults = milvusClient.search(searchReq).getSearchResults();
        Set<Long> recallResults = new HashSet<>();
        for (SearchResp.SearchResult result : searchResults.get(0)) {
            recallResults.add((Long) result.getId());
        }
        return recallResults;
    }

    private static List<Float> generateRandomVector() {
        List<Float> vector = new ArrayList<>(1024);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 1024; i++) {
            float value = (float) random.nextDouble(-0.08, 0.08);
            vector.add(value);
        }
        return vector;
    }
}
