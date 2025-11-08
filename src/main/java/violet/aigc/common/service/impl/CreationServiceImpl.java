package violet.aigc.common.service.impl;

import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import violet.aigc.common.mapper.CreationMapper;
import violet.aigc.common.pojo.Creation;
import violet.aigc.common.proto_gen.aigc.*;
import violet.aigc.common.proto_gen.common.BaseResp;
import violet.aigc.common.proto_gen.common.StatusCode;
import violet.aigc.common.service.CreationService;
import violet.aigc.common.utils.SnowFlake;

import java.util.*;

@Slf4j
@Service
public class CreationServiceImpl implements CreationService {
    @Autowired
    private CreationMapper creationMapper;
    private final SnowFlake creationIdGenerator = new SnowFlake(0, 0);

    @Override
    public CreateCreationResponse createCreation(CreateCreationRequest req) {
        CreateCreationResponse.Builder resp = CreateCreationResponse.newBuilder();
        Long creationId = creationIdGenerator.nextId();
        Date now = new Date();
        Creation creation = new Creation(null, creationId, req.getUserId(), req.getMaterialId(), req.getMaterialType(), req.getMaterialUrl(), req.getTitle(), req.getContent(), req.getCategory(), now, now, 0, "" );
        if (!creationMapper.insertCreation(creation)) {
            log.error("作品入库失败，作品ID：{}", creationId);
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public DeleteCreationResponse deleteCreation(DeleteCreationRequest req) {
        DeleteCreationResponse.Builder resp = DeleteCreationResponse.newBuilder();
        if (!creationMapper.deleteCreation(req.getCreationId())) {
            log.error("作品删除失败，作品ID：{}", req.getCreationId());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public GetCreationByIdResponse getCreationById(GetCreationByIdRequest req) {
        GetCreationByIdResponse.Builder resp = GetCreationByIdResponse.newBuilder();
        Creation creation = creationMapper.selectByCreationId(req.getCreationId());
        if (creation == null) {
            log.error("作品不存在，作品ID：{}", req.getCreationId());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Not_Found_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public GetCreationsByUserResponse getCreationsByUser(GetCreationsByUserRequest req) {
        //todo:图
        return null;
    }

    @Override
    public GetCreationsByRecResponse getCreationsByRec(GetCreationsByRecRequest req) {
        //todo：图、milvus、redis热度？多路召回
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .uri("http://localhost:19530")
                .token("root:Milvus")
                .build());

        GetReq getReq = GetReq.builder()
                .collectionName("my_collection")
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

    @Override
    public GetCreationsBySearchResponse getCreationsBySearch(GetCreationsBySearchRequest req) {
        //todo：milvus？
        ConnectConfig config = ConnectConfig.builder()
                .uri("http://localhost:19530")
                .token("root:Milvus")
                .build();
        MilvusClientV2 client = new MilvusClientV2(config);
        float[] queryDense = new float[]{-0.0475336798f,  0.0521207601f,  0.0904406682f};
        List<BaseVector> queryTexts = Collections.singletonList(new EmbeddedText("white headphones, quiet and comfortable"));
        List<BaseVector> queryDenseVectors = Collections.singletonList(new FloatVec(queryDense));
        List<AnnSearchReq> searchRequests = new ArrayList<>();
        searchRequests.add(AnnSearchReq.builder()
                .vectorFieldName("text_dense")
                .vectors(queryDenseVectors)
                .params("{\"nprobe\": 10}")
                .topK(2)
                .build());
        searchRequests.add(AnnSearchReq.builder()
                .vectorFieldName("text_sparse")
                .vectors(queryTexts)
                .params("{\"drop_ratio_search\": 0.2}")
                .topK(2)
                .build());
        CreateCollectionReq.Function ranker = CreateCollectionReq.Function.builder()
                .name("rrf")
                .functionType(FunctionType.RERANK)
                .param("reranker", "rrf")
                .param("k", "100")
                .build();
        HybridSearchReq hybridSearchReq = HybridSearchReq.builder()
                .collectionName("creation")
                .searchRequests(searchRequests)
                .ranker(ranker)
                .topK(2)
                .build();
        SearchResp searchResp = client.hybridSearch(hybridSearchReq);
        return null;
    }
}
