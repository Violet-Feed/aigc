package violet.creation.common;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import violet.creation.common.proto_gen.creation.*;
import violet.creation.common.service.MaterialService;

@GrpcService
public class CreationService extends CreationServiceGrpc.CreationServiceImplBase {
    @Autowired
    private MaterialService materialService;
    @Autowired
    private violet.creation.common.service.CreationService creationService;

    @Override
    public void createMaterial(CreateMaterialRequest request, StreamObserver<CreateMaterialResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.createMaterial(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteMaterial(DeleteMaterialRequest request, StreamObserver<DeleteMaterialResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.deleteMaterial(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getMaterialByUser(GetMaterialByUserRequest request, StreamObserver<GetMaterialByUserResponse> responseObserver) {
        try {
            responseObserver.onNext(materialService.getMaterialByUser(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void createCreation(CreateCreationRequest request, StreamObserver<CreateCreationResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.createCreation(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void deleteCreation(DeleteCreationRequest request, StreamObserver<DeleteCreationResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.deleteCreation(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationById(GetCreationByIdRequest request, StreamObserver<GetCreationByIdResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationById(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsByUser(GetCreationsByUserRequest request, StreamObserver<GetCreationsByUserResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsByUser(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsByRec(GetCreationsByRecRequest request, StreamObserver<GetCreationsByRecResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsByRec(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCreationsBySearch(GetCreationsBySearchRequest request, StreamObserver<GetCreationsBySearchResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.getCreationsBySearch(request));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
}
