package violet.aigc.common.service.rec;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class EmbeddingRecall {
    @Autowired
    private MilvusClientV2 milvusClient;

    private static final int RECALL_SIZE = 10;

    public Set<Long> recall(Set<Long> triggerIds) {
        GetReq getReq = GetReq.builder()
                .collectionName("creation")
                .ids(new ArrayList<>(triggerIds))
                .outputFields(Collections.singletonList("rec_embeddings"))
                .build();
        GetResp getResp = milvusClient.get(getReq);
        List<QueryResp.QueryResult> queryResults = getResp.getGetResults();
        List<BaseVector> queryEmbedding = new ArrayList<>();
        for (QueryResp.QueryResult queryResult : queryResults) {
            List<Float> embList = (List<Float>) queryResult.getEntity().get("rec_embeddings");
            queryEmbedding.add(new FloatVec(embList));
        }
        if (queryEmbedding.isEmpty()) {
            return Collections.emptySet();
        }

        SearchReq searchReq = SearchReq.builder()
                .collectionName("creation")
                .data(queryEmbedding)
                .annsField("rec_embeddings")
                .topK(RECALL_SIZE)
                .build();
        List<List<SearchResp.SearchResult>> searchResults = milvusClient.search(searchReq).getSearchResults();
        Set<Long> recallResults = new HashSet<>();
        for (List<SearchResp.SearchResult> searchResult : searchResults) {
            for (SearchResp.SearchResult result : searchResult) {
                recallResults.add((Long) result.getId());
            }
        }
        return recallResults;
    }
}
