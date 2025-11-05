package violet.creation.common.service;

import violet.creation.common.proto_gen.creation.*;

public interface MaterialService {
    CreateMaterialResponse createMaterial(CreateMaterialRequest req);

    VideoMaterialCallbackResponse videoMaterialCallback(VideoMaterialCallbackRequest req);

    DeleteMaterialResponse deleteMaterial(DeleteMaterialRequest req);

    GetMaterialByUserResponse getMaterialByUser(GetMaterialByUserRequest req);
}
