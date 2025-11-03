package violet.creation.common.service;

import violet.creation.common.proto_gen.creation.*;

public interface CreationService {
    CreateCreationResponse createCreation(CreateCreationRequest req);

    DeleteCreationResponse deleteCreation(DeleteCreationRequest req);

    GetCreationByIdResponse getCreationById(GetCreationByIdRequest req);

    GetCreationsByUserResponse getCreationsByUser(GetCreationsByUserRequest req);

    GetCreationsByRecResponse getCreationsByRec(GetCreationsByRecRequest req);

    GetCreationsBySearchResponse getCreationsBySearch(GetCreationsBySearchRequest req);
}
