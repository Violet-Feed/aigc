package violet.creation.common.service;

import violet.creation.common.proto_gen.creation.*;

public interface CreationService {
    CreateCreationResponse createCreation(CreateCreationRequest req);

    PublishCreationResponse publishCreation(PublishCreationRequest req);

    GetCreationByIdResponse getCreationById(GetCreationByIdRequest req);
}
