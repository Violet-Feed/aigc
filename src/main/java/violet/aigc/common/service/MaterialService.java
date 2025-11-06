package violet.aigc.common.service;

import violet.aigc.common.proto_gen.aigc.*;

public interface MaterialService {
    CreateMaterialResponse createMaterial(CreateMaterialRequest req);

    VideoMaterialCallbackResponse videoMaterialCallback(VideoMaterialCallbackRequest req);

    DeleteMaterialResponse deleteMaterial(DeleteMaterialRequest req);

    GetMaterialByUserResponse getMaterialByUser(GetMaterialByUserRequest req);
}
