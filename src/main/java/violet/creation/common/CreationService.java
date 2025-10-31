package violet.creation.common;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import violet.creation.common.proto_gen.creation.*;

@GrpcService
public class CreationService extends CreationServiceGrpc.CreationServiceImplBase {
    @Autowired
    private violet.creation.common.service.CreationService creationService;

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
    public void publishCreation(PublishCreationRequest request, StreamObserver<PublishCreationResponse> responseObserver) {
        try {
            responseObserver.onNext(creationService.publishCreation(request));
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
}
