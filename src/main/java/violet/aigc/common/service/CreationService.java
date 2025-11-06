package violet.aigc.common.service;

import violet.aigc.common.proto_gen.aigc.*;

public interface CreationService {
    CreateCreationResponse createCreation(CreateCreationRequest req);

    DeleteCreationResponse deleteCreation(DeleteCreationRequest req);

    GetCreationByIdResponse getCreationById(GetCreationByIdRequest req);

    GetCreationsByUserResponse getCreationsByUser(GetCreationsByUserRequest req);

    GetCreationsByRecResponse getCreationsByRec(GetCreationsByRecRequest req);

    GetCreationsBySearchResponse getCreationsBySearch(GetCreationsBySearchRequest req);
}
