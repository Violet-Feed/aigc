package violet.aigc.common.service.impl;

import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import violet.aigc.common.mapper.CreationGraphMapper;
import violet.aigc.common.mapper.CreationMapper;
import violet.aigc.common.pojo.Creation;
import violet.aigc.common.pojo.RecallResult;
import violet.aigc.common.proto_gen.aigc.*;
import violet.aigc.common.proto_gen.common.BaseResp;
import violet.aigc.common.proto_gen.common.StatusCode;
import violet.aigc.common.service.CreationService;
import violet.aigc.common.service.recall.*;
import violet.aigc.common.utils.QwenUtil;
import violet.aigc.common.utils.SnowFlake;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CreationServiceImpl implements CreationService {
    @Autowired
    private CreationMapper creationMapper;
    @Autowired
    private CreationGraphMapper creationGraphMapper;
    @Autowired
    private TriggerRetrieval triggerRetrieval;
    @Autowired
    private FilterRetrieval filterRetrieval;
    @Autowired
    private SwingRecall swingRecall;
    @Autowired
    private HotRecall hotRecall;
    @Autowired
    private EmbeddingRecall embeddingRecall;
    @Autowired
    private SimpleRanker simpleRanker;

    private final SnowFlake creationIdGenerator = new SnowFlake(0, 0);

    private ExecutorService asyncExecutor;

    @PostConstruct
    public void init() {
        int corePoolSize = 5;
        int maxPoolSize = 10;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Recall-Thread-" + (++count));
            }
        };
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        asyncExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public CreateCreationResponse createCreation(CreateCreationRequest req) {
        CreateCreationResponse.Builder resp = CreateCreationResponse.newBuilder();
        Long creationId = creationIdGenerator.nextId();
        Date now = new Date();
        Creation creation = new Creation(null, creationId, req.getUserId(), req.getMaterialId(), req.getMaterialType(), req.getMaterialUrl(), req.getTitle(), req.getContent(), req.getCategory(), now, now, 0, "");
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
        GetCreationsByUserResponse.Builder resp = GetCreationsByUserResponse.newBuilder();
        List<Creation> creations = creationGraphMapper.getCreationsByUser(req.getUserId(), req.getPage());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public GetCreationsByRecResponse getCreationsByRec(GetCreationsByRecRequest req) {
        GetCreationsByRecResponse.Builder resp = GetCreationsByRecResponse.newBuilder();
        try {
            CompletableFuture<List<Long>> triggerFuture = CompletableFuture.supplyAsync(() -> triggerRetrieval.retrieve(req.getUserId()), asyncExecutor);
            CompletableFuture<List<Long>> filterFuture = CompletableFuture.supplyAsync(() -> filterRetrieval.retrieve(req.getUserId()), asyncExecutor);
            List<Long> triggerIds = triggerFuture.get();
            CompletableFuture<List<RecallResult>> embeddingFuture = CompletableFuture.supplyAsync(() -> embeddingRecall.recall(triggerIds), asyncExecutor);
            CompletableFuture<List<RecallResult>> hotFuture = CompletableFuture.supplyAsync(() -> hotRecall.recall(triggerIds), asyncExecutor);
            CompletableFuture<List<RecallResult>> swingFuture = CompletableFuture.supplyAsync(() -> swingRecall.recall(triggerIds), asyncExecutor);
            List<RecallResult> allRecallResults = new ArrayList<>();
            allRecallResults.addAll(embeddingFuture.get());
            allRecallResults.addAll(hotFuture.get());
            allRecallResults.addAll(swingFuture.get());
            List<Long> filterIds = filterFuture.get();
            List<Long> rankedIds = simpleRanker.rank(allRecallResults, filterIds);
            List<Creation> creations = creationMapper.selectByCreationIds(rankedIds);
        } catch (InterruptedException | ExecutionException e) {
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public GetCreationsBySearchResponse getCreationsBySearch(GetCreationsBySearchRequest req) {
        GetCreationsBySearchResponse.Builder resp = GetCreationsBySearchResponse.newBuilder();
        ConnectConfig config = ConnectConfig.builder()
                .uri("http://localhost:19530")
                .token("root:Milvus")
                .build();
        MilvusClientV2 client = new MilvusClientV2(config);
        List<Float> keywordEmbedding = QwenUtil.getTextEmbedding(req.getKeyword());
        List<BaseVector> queryTexts = Collections.singletonList(new EmbeddedText(req.getKeyword()));
        List<BaseVector> queryDenseVectors = Collections.singletonList(new FloatVec(keywordEmbedding));
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
        List<List<SearchResp.SearchResult>> searchResults = client.hybridSearch(hybridSearchReq).getSearchResults();
        List<RecallResult> recallResults = new ArrayList<>();
        if (!searchResults.isEmpty()) {
            recallResults = searchResults.get(0).stream()
                    .map(searchResult -> new RecallResult(null, (Long) searchResult.getEntity().get("creation_id"), searchResult.getScore().doubleValue()))
                    .collect(Collectors.toList());
        }
        List<Long> creationIds = recallResults.stream().map(RecallResult::getId).collect(Collectors.toList());
        List<Creation> creations = creationMapper.selectByCreationIds(creationIds);
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).build();
    }
}
