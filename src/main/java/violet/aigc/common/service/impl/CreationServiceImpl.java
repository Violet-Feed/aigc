package violet.aigc.common.service.impl;

import com.alibaba.fastjson2.JSON;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import violet.aigc.common.mapper.CreationGraphMapper;
import violet.aigc.common.mapper.CreationMapper;
import violet.aigc.common.pojo.Creation;
import violet.aigc.common.proto_gen.action.ActionServiceGrpc;
import violet.aigc.common.proto_gen.action.GetDiggListByUserRequest;
import violet.aigc.common.proto_gen.action.GetDiggListByUserResponse;
import violet.aigc.common.proto_gen.aigc.*;
import violet.aigc.common.proto_gen.common.BaseResp;
import violet.aigc.common.proto_gen.common.StatusCode;
import violet.aigc.common.service.CreationService;
import violet.aigc.common.service.rec.*;
import violet.aigc.common.utils.QwenUtil;
import violet.aigc.common.utils.SnowFlake;
import violet.aigc.common.utils.TimeUtil;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CreationServiceImpl implements CreationService {
    @GrpcClient("action")
    private ActionServiceGrpc.ActionServiceBlockingStub actionStub;
    @Autowired
    private MilvusClientV2 milvusClient;
    @Autowired
    private CreationMapper creationMapper;
    @Autowired
    private CreationGraphMapper creationGraphMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private TriggerRetrieval triggerRetrieval;
    @Autowired
    private FilterRetrieval filterRetrieval;
    @Autowired
    private SwingRecall swingRecall;
    @Autowired
    private TrendRecall trendRecall;
    @Autowired
    private EmbeddingRecall embeddingRecall;
    @Autowired
    private BackupRecall backupRecall;
    @Autowired
    private BeforeRanker beforeRanker;
    @Autowired
    private RandomRanker randomRanker;

    private final SnowFlake creationIdGenerator = new SnowFlake(0, 0);
    private ExecutorService asyncExecutor;
    private static final int PAGE_SIZE = 20;


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
        String coverUrl = "";
        if (req.getMaterialType() == MaterialType.Image_VALUE) {
            coverUrl = req.getMaterialUrl();
        }
        //todo:获取首帧
        Date now = new Date();
        Creation creation = new Creation(null, creationId, req.getUserId(), coverUrl, req.getMaterialId(), req.getMaterialType(), req.getMaterialUrl(), req.getTitle(), req.getContent(), req.getCategory(), now, now, 0, "");
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
        return resp.setBaseResp(baseResp).setCreation(creation.toProto()).build();
    }

    @Override
    public GetCreationsByUserResponse getCreationsByUser(GetCreationsByUserRequest req) {
        GetCreationsByUserResponse.Builder resp = GetCreationsByUserResponse.newBuilder();
        List<Long> creationIds = creationGraphMapper.getCreationIdsByUser(req.getUserId(), req.getPage());
        if (creationIds.isEmpty()) {
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).build();
        }
        List<Creation> creations = creationMapper.selectByCreationIds(creationIds);
        List<violet.aigc.common.proto_gen.aigc.Creation> creationDto = creations.stream().map(Creation::toProto).collect(Collectors.toList());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllCreations(creationDto).build();
    }

    @Override
    public GetCreationsByDiggResponse getCreationsByDigg(GetCreationsByDiggRequest req) {
        GetCreationsByDiggResponse.Builder resp = GetCreationsByDiggResponse.newBuilder();
        GetDiggListByUserRequest getDiggListByUserRequest = GetDiggListByUserRequest.newBuilder()
                .setUserId(req.getUserId())
                .setEntityType("creation")
                .setPage(req.getPage())
                .build();
        GetDiggListByUserResponse getDiggListByUserResponse = actionStub.getDiggListByUser(getDiggListByUserRequest);
        if (getDiggListByUserResponse.getBaseResp().getStatusCode() != StatusCode.Success) {
            log.error("[getCreationsByDigg] getDiggListByUser rpc err, err = {}", getDiggListByUserResponse.getBaseResp());
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
            return resp.setBaseResp(baseResp).build();
        }
        List<Long> creationIds = getDiggListByUserResponse.getEntityIdsList();
        if (creationIds.isEmpty()) {
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).build();
        }
        List<Creation> creations = creationMapper.selectByCreationIds(creationIds);
        List<violet.aigc.common.proto_gen.aigc.Creation> creationDto = creations.stream().map(Creation::toProto).collect(Collectors.toList());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllCreations(creationDto).build();
    }

    @Override
    public GetCreationsByFriendResponse getCreationsByFriend(GetCreationsByFriendRequest req) {
        GetCreationsByFriendResponse.Builder resp = GetCreationsByFriendResponse.newBuilder();
        List<Long> creationIds = creationGraphMapper.getCreationIdsByFriend(req.getUserId(), req.getPage());
        if (creationIds.isEmpty()) {
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).build();
        }
        List<Creation> creations = creationMapper.selectByCreationIds(creationIds);
        List<violet.aigc.common.proto_gen.aigc.Creation> creationDto = creations.stream().map(Creation::toProto).collect(Collectors.toList());
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllCreations(creationDto).build();
    }

    @Override
    public GetCreationsByRecResponse getCreationsByRec(GetCreationsByRecRequest req) {
        GetCreationsByRecResponse.Builder resp = GetCreationsByRecResponse.newBuilder();
        Long userId = req.getUserId();
        try {
            CompletableFuture<Set<Long>> triggerFuture = CompletableFuture.supplyAsync(() -> triggerRetrieval.retrieve(userId), asyncExecutor);
            CompletableFuture<Set<Long>> filterFuture = CompletableFuture.supplyAsync(() -> filterRetrieval.retrieve(userId), asyncExecutor);
            CompletableFuture<Set<Long>> backupFuture = CompletableFuture.supplyAsync(() -> backupRecall.recall(), asyncExecutor);
            Set<Long> triggerIds = triggerFuture.get();

            CompletableFuture<Set<Long>> swingFuture;
            CompletableFuture<Set<Long>> embeddingFuture;
            if (triggerIds.isEmpty()) {
                swingFuture = CompletableFuture.completedFuture(Collections.emptySet());
                embeddingFuture = CompletableFuture.completedFuture(Collections.emptySet());
            } else {
                swingFuture = CompletableFuture.supplyAsync(() -> swingRecall.recall(triggerIds), asyncExecutor);
                embeddingFuture = CompletableFuture.supplyAsync(() -> embeddingRecall.recall(triggerIds), asyncExecutor);
            }
            CompletableFuture<Set<Long>> trendFuture = CompletableFuture.supplyAsync(() -> trendRecall.recall(triggerIds), asyncExecutor);

            CompletableFuture.allOf(filterFuture, backupFuture, swingFuture, embeddingFuture, trendFuture).join();
            Set<Long> swingResults     = swingFuture.get();
            Set<Long> embeddingResults = embeddingFuture.get();
            Set<Long> trendResults     = trendFuture.get();
            Set<Long> backupIds        = backupFuture.get();
            Set<Long> filterIds        = filterFuture.get();

            Set<Long> allRecallResults = new HashSet<>();
            allRecallResults.addAll(swingResults);
            allRecallResults.addAll(embeddingResults);
            allRecallResults.addAll(trendResults);
            Set<Long> beforeRankResults = beforeRanker.execute(allRecallResults, filterIds, backupIds);
            List<Long> rankedResults    = randomRanker.rank(beforeRankResults);

            List<Creation> creations = creationMapper.selectByCreationIds(rankedResults);
            List<violet.aigc.common.proto_gen.aigc.Creation> creationDto = creations.stream().map(Creation::toProto).collect(Collectors.toList());

            String expoKey = "expo:" + userId + ":" + TimeUtil.getNowDate();
            String expoValue = rankedResults.stream().map(String::valueOf).collect(Collectors.joining(","));
            redisTemplate.opsForList().rightPush(expoKey, expoValue);

            log.info("[getCreationsByRec] userId={}, triggerIds={}, filterIds={}, " +
                            "swingResults={}, embeddingResults={}, trendResults={}, " +
                            "backupIds={}, recallIds={}, beforeRankIds={}, finalIds={}",
                    userId, triggerIds, filterIds, swingResults, embeddingResults, trendResults, backupIds, allRecallResults, beforeRankResults, rankedResults
            );
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).addAllCreations(creationDto).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[getCreationsByRec] interrupted, userId={}", req.getUserId(), e);
        } catch (ExecutionException e) {
            log.error("[getCreationsByRec] execution error, userId={}", req.getUserId(), e);
        }
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Server_Error).build();
        return resp.setBaseResp(baseResp).build();
    }

    @Override
    public GetCreationsBySearchResponse getCreationsBySearch(GetCreationsBySearchRequest req) {
        GetCreationsBySearchResponse.Builder resp = GetCreationsBySearchResponse.newBuilder();
        int offset = (req.getPage() - 1) * PAGE_SIZE;
        String cacheKey = "search:" + req.getKeyword();
        List<Long> creationIds;
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            creationIds = JSON.parseArray(cacheValue, Long.class);
        } else {
            List<Float> keywordEmbedding = QwenUtil.getTextEmbedding(req.getKeyword());
            List<BaseVector> queryEmbedding = Collections.singletonList(new FloatVec(keywordEmbedding));
            List<BaseVector> queryTexts = Collections.singletonList(new EmbeddedText(req.getKeyword()));
            List<AnnSearchReq> searchRequests = new ArrayList<>();
            searchRequests.add(AnnSearchReq.builder()
                    .vectorFieldName("rec_embeddings")
                    .vectors(queryEmbedding)
                    .params("{\"ef\": 10}")
                    .topK(200)
                    .build());
            searchRequests.add(AnnSearchReq.builder()
                    .vectorFieldName("title_embeddings")
                    .vectors(queryTexts)
                    .params("{\"drop_ratio_search\": 0.2}")
                    .topK(300)
                    .build());
            CreateCollectionReq.Function ranker = CreateCollectionReq.Function.builder()
                    .name("rrf")
                    .functionType(FunctionType.RERANK)
                    .param("reranker", "rrf")
                    .param("k", "60")
                    .build();
            HybridSearchReq hybridSearchReq = HybridSearchReq.builder()
                    .collectionName("creation")
                    .searchRequests(searchRequests)
                    .ranker(ranker)
                    .topK(400)
                    .build();
            SearchResp searchResp = milvusClient.hybridSearch(hybridSearchReq);
            List<List<SearchResp.SearchResult>> searchResults = searchResp.getSearchResults();
            creationIds = new ArrayList<>();
            if (!searchResults.isEmpty()) {
                for (SearchResp.SearchResult result : searchResults.get(0)) {
                    creationIds.add((Long) result.getId());
                }
            }
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(creationIds), 10, TimeUnit.MINUTES);
        }
        if (offset >= creationIds.size()) {
            BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
            return resp.setBaseResp(baseResp).build();
        }
        int toIndex = Math.min(offset + PAGE_SIZE, creationIds.size());
        List<Long> pageIds = creationIds.subList(offset, toIndex);
        log.info("[getCreationsBySearch] keyword={}, totalIds={}, offset={}, toIndex={}, pageIds={}", req.getKeyword(), creationIds.size(), offset, toIndex, pageIds);
        List<Creation> creations = creationMapper.selectByCreationIds(pageIds);
        Map<Long, Creation> creationMap = creations.stream().collect(Collectors.toMap(Creation::getCreationId, Function.identity()));
        List<violet.aigc.common.proto_gen.aigc.Creation> creationDto = pageIds.stream().map(creationMap::get).filter(Objects::nonNull).map(Creation::toProto).collect(Collectors.toList());
        boolean hasMore = toIndex < creationIds.size();
        BaseResp baseResp = BaseResp.newBuilder().setStatusCode(StatusCode.Success).build();
        return resp.setBaseResp(baseResp).addAllCreations(creationDto).build();
    }
}
