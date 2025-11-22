package violet.aigc.common.service.recall;

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
public class EmbeddingRecall implements CommonRecall {
    @Autowired
    private MilvusClientV2 milvusClient;

    @Override
    public Set<Long> recall(Set<Long> triggerIds) {
        GetReq getReq = GetReq.builder()
                .collectionName("creation")
                .ids(new ArrayList<>(triggerIds))
                .outputFields(Collections.singletonList("title_embedding"))
                .build();
        GetResp getResp = milvusClient.get(getReq);
        List<QueryResp.QueryResult> queryResults = getResp.getGetResults();
        List<BaseVector> queryEmbedding = new ArrayList<>();
        for (QueryResp.QueryResult queryResult : queryResults) {
            queryEmbedding.add((FloatVec) queryResult.getEntity().get("title_embedding"));
        }

        SearchReq searchReq = SearchReq.builder()
                .collectionName("creation")
                .data(queryEmbedding)
                .topK(100)
                .build();
        List<List<SearchResp.SearchResult>> searchResults = milvusClient.search(searchReq).getSearchResults();
        Set<Long> recallResults = new HashSet<>();
        for (List<SearchResp.SearchResult> searchResult : searchResults) {
            for (SearchResp.SearchResult result : searchResult) {
                recallResults.add((Long) result.getEntity().get("creation_id"));
            }
        }
        return recallResults;
    }
}
