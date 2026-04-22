package violet.aigc.common;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import violet.aigc.common.proto_gen.aigc.*;
import violet.aigc.common.service.AgentService;
import violet.aigc.common.service.CreationService;
import violet.aigc.common.service.MaterialService;

@Slf4j
@GrpcService
public class AigcService extends AigcServiceGrpc.AigcServiceImplBase {
    @Autowired
    private MaterialService materialService;
    @Autowired
    private CreationService creationService;
    @Autowired
    private AgentService agentService;

    @Override
    public void createMaterial(CreateMaterialRequest request, StreamObserver<CreateMaterialResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.createMaterial(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("createMaterial error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void videoMaterialCallback(VideoMaterialCallbackRequest request, StreamObserver<VideoMaterialCallbackResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.videoMaterialCallback(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("videoMaterialCallback error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteMaterial(DeleteMaterialRequest request, StreamObserver<DeleteMaterialResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.deleteMaterial(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("deleteMaterial error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getMaterialByUser(GetMaterialByUserRequest request, StreamObserver<GetMaterialByUserResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.getMaterialByUser(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getMaterialByUser error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void createCreation(CreateCreationRequest request, StreamObserver<CreateCreationResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.createCreation(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("createCreation error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteCreation(DeleteCreationRequest request, StreamObserver<DeleteCreationResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.deleteCreation(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("deleteCreation error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateCreation(UpdateCreationRequest request, StreamObserver<UpdateCreationResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.updateCreation(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("updateCreation error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationById(GetCreationByIdRequest request, StreamObserver<GetCreationByIdResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationById(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationById error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationByIds(GetCreationByIdsRequest request, StreamObserver<GetCreationByIdsResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationByIds(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationByIds error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsByUser(GetCreationsByUserRequest request, StreamObserver<GetCreationsByUserResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsByUser(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationsByUser error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsByDigg(GetCreationsByDiggRequest request, StreamObserver<GetCreationsByDiggResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsByDigg(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationsByDigg error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsByFriend(GetCreationsByFriendRequest request, StreamObserver<GetCreationsByFriendResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsByFriend(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationsByFriend error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsByRec(GetCreationsByRecRequest request, StreamObserver<GetCreationsByRecResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsByRec(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationsByRec error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsBySearch(GetCreationsBySearchRequest request, StreamObserver<GetCreationsBySearchResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsBySearch(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getCreationsBySearch error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void createAgent(CreateAgentRequest request, StreamObserver<CreateAgentResponse> responseObserver) {
        try {
            responseObserver.onNext(agentService.createAgent(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("createAgent error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteAgent(DeleteAgentRequest request, StreamObserver<DeleteAgentResponse> responseObserver) {
        try {
            responseObserver.onNext(agentService.deleteAgent(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("deleteAgent error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void updateAgent(UpdateAgentRequest request, StreamObserver<UpdateAgentResponse> responseObserver) {
        try {
            responseObserver.onNext(agentService.updateAgent(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("updateAgent error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAgentsByIds(GetAgentsByIdsRequest request, StreamObserver<GetAgentsByIdsResponse> responseObserver) {
        try {
            responseObserver.onNext(agentService.getAgentsByIds(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getAgentsByIds error", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getAgentsByUser(GetAgentsByUserRequest request, StreamObserver<GetAgentsByUserResponse> responseObserver) {
        try {
            responseObserver.onNext(agentService.getAgentsByUser(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("getAgentsByUser error", e);
            responseObserver.onError(e);
        }
    }
}
