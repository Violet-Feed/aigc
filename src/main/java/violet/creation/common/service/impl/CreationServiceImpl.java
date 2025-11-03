package violet.creation.common.service.impl;

import org.springframework.stereotype.Service;
import violet.creation.common.proto_gen.creation.*;
import violet.creation.common.service.CreationService;

@Service
public class CreationServiceImpl implements CreationService {
    @Override
    public CreateCreationResponse createCreation(CreateCreationRequest req) {
        return null;
    }

    @Override
    public DeleteCreationResponse deleteCreation(DeleteCreationRequest req) {
        return null;
    }

    @Override
    public GetCreationByIdResponse getCreationById(GetCreationByIdRequest req) {
        return null;
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
