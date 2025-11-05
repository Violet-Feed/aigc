package violet.creation.common.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import violet.creation.common.mapper.CreationMapper;
import violet.creation.common.pojo.Creation;
import violet.creation.common.proto_gen.common.BaseResp;
import violet.creation.common.proto_gen.common.StatusCode;
import violet.creation.common.proto_gen.creation.*;
import violet.creation.common.service.CreationService;
import violet.creation.common.utils.SnowFlake;

import java.util.Date;

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
        return null;
    }

    @Override
    public GetCreationsByRecResponse getCreationsByRec(GetCreationsByRecRequest req) {
        return null;
    }

    @Override
    public GetCreationsBySearchResponse getCreationsBySearch(GetCreationsBySearchRequest req) {
        return null;
    }
}
